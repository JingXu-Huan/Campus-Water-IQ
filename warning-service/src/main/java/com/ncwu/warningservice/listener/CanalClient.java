package com.ncwu.warningservice.listener;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.ncwu.warningservice.emailservice.EmailService;
import com.ncwu.warningservice.wechatservice.WeChatNotifyService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class CanalClient {

    private CanalConnector connector;
    private final RocketMQTemplate rocketMQTemplate;
    private final WeChatNotifyService weChatNotifyService;
    private final EmailService emailService;

    @PostConstruct
    public void init() {
        // 连接到Canal Server
        connector = CanalConnectors.newSingleConnector(
                new InetSocketAddress("127.0.0.1", 11111),
                "example",  // destination，对应conf/example
                "",
                ""
        );

        // 启动监听线程
        new Thread(this::startListen).start();
    }

    @PreDestroy
    public void destroy() {
        if (connector != null) {
            connector.disconnect();
        }
    }

    private void startListen() {
        try {
            connector.connect();
            //监听的表
            connector.subscribe("water\\.iot_device_event,water\\.device_reservation");
            connector.rollback();

            while (true) {
                Message message = connector.getWithoutAck(100); // 获取100条数�?
                long batchId = message.getId();

                if (batchId == -1 || message.getEntries().isEmpty()) {
                    Thread.sleep(1000);
                    continue;
                }

                processEntries(message.getEntries());
                connector.ack(batchId); // 确认消费
            }
        } catch (Exception e) {
            log.error("Canal监听异常", e);
        }
    }

    private void processEntries(List<CanalEntry.Entry> entries) {
        for (CanalEntry.Entry entry : entries) {
            if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
                continue;
            }
            try {
                CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                CanalEntry.EventType eventType = rowChange.getEventType();

                // 只处理INSERT事件
                if (eventType == CanalEntry.EventType.INSERT) {
                    String tableName = entry.getHeader().getTableName();

                    for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                        Map<String, String> dataMap = new HashMap<>();

                        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
                            dataMap.put(column.getName(), column.getValue());
                        }

                        log.info("监听到新增数�?- �? {}, 数据: {}", tableName, dataMap);

                        // 发送到RabbitMQ或直接处�?
                        handleNewData(tableName, dataMap);
                    }
                }
            } catch (Exception e) {
                log.error("解析binlog失败", e);
            }
        }
    }

    private void handleNewData(String tableName, Map<String, String> data) throws MessagingException {
        if (tableName.equals("iot_device_event")) {
            String deviceCode = data.get("device_code");
            String eventTime = data.get("event_time");
            String deviceType = data.get("device_type");
            String eventLevel = data.get("event_level");
            String eventDesc = data.get("event_desc");
            String eventType = data.get("event_type");
            String suggestion;
            if (eventType.equals("ABNORMAL")) {
                suggestion = "请断电重启，并且检查传感器是否正常工作。";
            } else if (eventType.equals("OFFLINE")) {
                suggestion = "设备离线，请将设备重新断电启动。";
            } else {
                suggestion = "设备数据异常，请检查设备和传感器是否正常工作。";
            }
            weChatNotifyService.sendMdText(deviceCode, eventLevel, eventDesc, eventTime, suggestion);
        } else if (tableName.equals("device_reservation")) {
            String deviceCode = data.get("device_code");
            String campusNo = data.get("campus_no");
            if (campusNo.equals("1")){
                campusNo = "花园校区";
            } else if (campusNo.equals("2")) {
                campusNo ="龙子湖校区";
            }
            else{
                campusNo = "江淮校区";
            }
            String buildingNo = data.get("building_no");
            String floorNo = data.get("floor_no");
            String unitNo = data.get("unit_no");
            String reporterName = data.get("reporter_name");
            String contactInfo = data.get("contact_info");
            String faultDesc = data.get("fault_desc");
            String severity = data.get("severity");
            String status = data.get("status");
            String remark = data.get("remark");
            String content = buildRepairEmailContent(
                    deviceCode,
                    campusNo,
                    buildingNo,
                    floorNo,
                    unitNo,
                    reporterName,
                    contactInfo,
                    faultDesc,
                    severity,
                    status,
                    remark
            );

            emailService.sendMail("📢 用户新报修单", content, "jingxushi13@gmail.com");
            //todo 给用户发短信
        }
    }

    private String buildRepairEmailContent(
            String deviceCode,
            String campusNo,
            String buildingNo,
            String floorNo,
            String unitNo,
            String reporterName,
            String contactInfo,
            String faultDesc,
            String severity,
            String status,
            String remark
    ) {
        return String.format("""
                        <html>
                        <body style="font-family: Arial, Helvetica, sans-serif; color: #333;">
                            <h2 style="color:#2c3e50;">📢 新报修单通知</h2>
                        
                            <table border="1" cellpadding="8" cellspacing="0"
                                   style="border-collapse: collapse; width: 100%%; font-size: 14px;">
                                <tr>
                                    <td><b>设备编码</b></td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td><b>校区</b></td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td><b>楼宇</b></td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td><b>楼层</b></td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td><b>单元</b></td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td><b>报修人</b></td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td><b>联系方式</b></td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td><b>故障描述</b></td>
                                    <td style="white-space: pre-wrap;">%s</td>
                                </tr>
                                <tr>
                                    <td><b>严重程度</b></td>
                                    <td style="color:%s; font-weight:bold;">%s</td>
                                </tr>
                                <tr>
                                    <td><b>状态</b></td>
                                    <td>%s</td>
                                </tr>
                                <tr>
                                    <td><b>备注</b></td>
                                    <td>%s</td>
                                </tr>
                            </table>
                        
                            <p style="margin-top:20px; font-size:12px; color:#888;">
                                本邮件由系统自动发送，请勿直接回复。
                            </p>
                        </body>
                        </html>
                        """,
                deviceCode,
                campusNo,
                buildingNo,
                floorNo,
                unitNo,
                reporterName,
                contactInfo,
                faultDesc,
                severityColor(severity),
                severityText(severity),
                status,
                remark
        );
    }
    private String severityColor(String severity) {
        return switch (severity) {
            case "1" -> "#27ae60"; // 一般
            case "2" -> "#e67e22"; // 较严重
            case "3" -> "#e74c3c"; // 紧急
            default -> "#333";
        };
    }

    private String severityText(String severity) {
        return switch (severity) {
            case "1" -> "一般";
            case "2" -> "严重";
            case "3" -> "紧急";
            default -> severity;
        };
    }
}
