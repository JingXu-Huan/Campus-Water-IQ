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
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>新报修单通知</title>
                        </head>
                        <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; 
                                      margin: 0; padding: 20px; background-color: #f8f9fa; color: #2c3e50; line-height: 1.6;">
                            <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; 
                                        border-radius: 12px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); overflow: hidden;">
                                
                                <!-- Header -->
                                <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px 20px; text-align: center;">
                                    <h1 style="margin: 0; color: #ffffff; font-size: 24px; font-weight: 600;">
                                        📢 新报修单通知
                                    </h1>
                                    <p style="margin: 10px 0 0 0; color: rgba(255, 255, 255, 0.9); font-size: 14px;">
                                        请及时处理以下报修请求
                                    </p>
                                </div>
                                
                                <!-- Content -->
                                <div style="padding: 30px 20px;">
                                    <table style="width: 100%%; border-collapse: collapse; font-size: 14px;">
                                        <tr>
                                            <td style="padding: 12px 0; border-bottom: 1px solid #e9ecef; color: #6c757d; font-weight: 500; width: 120px;">
                                                设备编码
                                            </td>
                                            <td style="padding: 12px 0; border-bottom: 1px solid #e9ecef; font-weight: 500;">%s</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 12px 0; border-bottom: 1px solid #e9ecef; color: #6c757d; font-weight: 500;">
                                                校区
                                            </td>
                                            <td style="padding: 12px 0; border-bottom: 1px solid #e9ecef;">%s</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 12px 0; border-bottom: 1px solid #e9ecef; color: #6c757d; font-weight: 500;">
                                                楼宇
                                            </td>
                                            <td style="padding: 12px 0; border-bottom: 1px solid #e9ecef;">%s</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 12px 0; border-bottom: 1px solid #e9ecef; color: #6c757d; font-weight: 500;">
                                                楼层
                                            </td>
                                            <td style="padding: 12px 0; border-bottom: 1px solid #e9ecef;">%s</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 12px 0; border-bottom: 1px solid #e9ecef; color: #6c757d; font-weight: 500;">
                                                单元
                                            </td>
                                            <td style="padding: 12px 0; border-bottom: 1px solid #e9ecef;">%s</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 12px 0; border-bottom: 1px solid #e9ecef; color: #6c757d; font-weight: 500;">
                                                报修人
                                            </td>
                                            <td style="padding: 12px 0; border-bottom: 1px solid #e9ecef;">%s</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 12px 0; border-bottom: 1px solid #e9ecef; color: #6c757d; font-weight: 500;">
                                                联系方式
                                            </td>
                                            <td style="padding: 12px 0; border-bottom: 1px solid #e9ecef;">%s</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 12px 0; border-bottom: 1px solid #e9ecef; color: #6c757d; font-weight: 500; vertical-align: top;">
                                                故障描述
                                            </td>
                                            <td style="padding: 12px 0; border-bottom: 1px solid #e9ecef; white-space: pre-wrap; line-height: 1.5;">%s</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 12px 0; border-bottom: 1px solid #e9ecef; color: #6c757d; font-weight: 500;">
                                                严重程度
                                            </td>
                                            <td style="padding: 12px 0; border-bottom: 1px solid #e9ecef;">
                                                <span style="display: inline-block; padding: 4px 12px; border-radius: 20px; 
                                                           font-size: 12px; font-weight: 600; color: %s; background-color: %s20;">
                                                    %s
                                                </span>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 12px 0; border-bottom: 1px solid #e9ecef; color: #6c757d; font-weight: 500;">
                                                状态
                                            </td>
                                            <td style="padding: 12px 0; border-bottom: 1px solid #e9ecef;">%s</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 12px 0; color: #6c757d; font-weight: 500; vertical-align: top;">
                                                备注
                                            </td>
                                            <td style="padding: 12px 0;">%s</td>
                                        </tr>
                                    </table>
                                </div>
                                
                                <!-- Footer -->
                                <div style="background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e9ecef;">
                                    <p style="margin: 0; font-size: 12px; color: #6c757d;">
                                        本邮件由系统自动发送，请勿直接回复
                                    </p>
                                    <p style="margin: 8px 0 0 0; font-size: 11px; color: #adb5bd;">
                                        © 2024 校园水务智能管理系统
                                    </p>
                                </div>
                            </div>
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
