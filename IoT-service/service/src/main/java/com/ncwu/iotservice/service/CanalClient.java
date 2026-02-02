package com.ncwu.iotservice.service;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import domain.dto.IoTDeviceEventDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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
            connector.subscribe("water\\.iot_device_event");
            connector.rollback();

            while (true) {
                Message message = connector.getWithoutAck(100); // 获取100条数据
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

                        log.info("监听到新增数据 - 表: {}, 数据: {}", tableName, dataMap);

                        // 发送到RabbitMQ或直接处理
                        handleNewData(tableName, dataMap);
                    }
                }
            } catch (Exception e) {
                log.error("解析binlog失败", e);
            }
        }
    }

    private void handleNewData(String tableName, Map<String, String> data) {
        IoTDeviceEventDTO ioTDeviceEventDTO = new IoTDeviceEventDTO();
        ioTDeviceEventDTO.setDeviceCode(data.get("device_code"));
        ioTDeviceEventDTO.setEventTime(data.get("event_time"));
        ioTDeviceEventDTO.setDeviceType(data.get("device_type"));
        ioTDeviceEventDTO.setEventLevel(data.get("event_level"));
        ioTDeviceEventDTO.setEventDesc(data.get("event_desc"));
        ioTDeviceEventDTO.setEventType(data.get("event_type"));
        rocketMQTemplate.convertAndSend("IoTDeviceEvent",ioTDeviceEventDTO);
    }
}