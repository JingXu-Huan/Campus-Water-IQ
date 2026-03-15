package com.ncwu.ingestgroup.consumer;


import com.influxdb.client.DeleteApi;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/15
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "InfluxDB", consumerGroup = "InfluxDBGroup")
public class InfluxDBOps implements RocketMQListener<String> {

    @Value("${influx.token}")
    private String influxToken;
    private InfluxDBClient influxDBClient;

    @PostConstruct
    public void init() {
        influxDBClient = InfluxDBClientFactory
                .create("http://localhost:8086", influxToken.toCharArray(), "ncwu", "water");
    }

    @Override
    public void onMessage(String message) {
        if (message.equals("rebuild")){
            clearData();
        }
    }

    private void clearData() {
        DeleteApi deleteApi = influxDBClient.getDeleteApi();
        // 删除 bucket 内所有数据（时间范围覆盖足够大）
        OffsetDateTime start = OffsetDateTime.parse("1970-01-01T00:00:00Z");
        OffsetDateTime stop  = OffsetDateTime.now(ZoneOffset.UTC).plusHours(1);
        // 删除所有 measurement（空 predicate = 不过滤）
        deleteApi.delete(start, stop, "", "water", "ncwu");
    }
}