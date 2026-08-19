package com.ncwu.iotservice.schedule;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxTable;
import com.ncwu.iotservice.config.ServiceConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务检测是否存在高流量异常
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckHighFlow {
    private final StringRedisTemplate redisTemplate;
    private final RocketMQTemplate rocketMQTemplate;
    private final ServiceConfig serviceConfig;
    private final InfluxDBClient influxDBClient;

    private QueryApi queryApi;

    private ScheduledExecutorService scheduler;

    private static final double HIGH_FLOW_THRESHOLD = 0.5;
    private static final String WATER_METER_PREFIX = "1";

    @PostConstruct
    public void init() {
        queryApi = influxDBClient.getQueryApi();
        scheduler = Executors.newScheduledThreadPool(5, Thread.ofVirtual().factory());
        scheduler.scheduleAtFixedRate(this::checkHighFlow, 0, 60, TimeUnit.SECONDS);
    }

    public void checkHighFlow() {
        Set<Object> ids = redisTemplate.opsForHash().entries("OnLineMap").keySet();
        StringBuilder deviceFilter = new StringBuilder();
        boolean first = true;
        for (Object id : ids) {
            String deviceId = id.toString();
            if (deviceId.startsWith(WATER_METER_PREFIX)) {
                if (!first) {
                    deviceFilter.append(" or ");
                }
                deviceFilter.append('r').append(".deviceId").append(" == ").append('"').append(deviceId).append('"');
                first = false;
            }
        }
        if (deviceFilter.isEmpty()) {
            return;
        }
        String flux = String.format("""
            from(bucket: "water")
              |> range(start: -1m)
              |> filter(fn: (r) =>
                r._measurement == "water_meter" and
                r._field == "flow" and
                (%s)
              )
            """, deviceFilter);
        try {
            for (FluxTable table : queryApi.query(flux)) {
                for (var record : table.getRecords()) {
                    Double value = record.getValue() != null ? ((Number) record.getValue()).doubleValue() : 0;
                    if (value > HIGH_FLOW_THRESHOLD) {
                        Object deviceIdValue = record.getValueByKey("deviceId");
                        if (deviceIdValue == null) {
                            continue;
                        }
                        String deviceId = String.valueOf(deviceIdValue);
                        log.warn("检测到高流量设备: {}, 流量: {}", deviceId, value);
                        rocketMQTemplate.convertAndSend("error-flow", buildHighFlowAlert(deviceId, value));
                    }
                }
            }
        } catch (Exception e) {
            log.error("高流量检测异常: {}", e.getMessage());
        }
    }

    private Object buildHighFlowAlert(String deviceId, double flow) {
        return java.util.Map.of(
                "deviceCode", deviceId,
                "deviceType", "METER",
                "alarmType", "HIGH_FLOW",
                "alarmLevel", "WARN",
                "originalValue", flow,
                "ruleCode", "FLOW_MAX_LIMIT",
                "ext", java.util.Map.of("threshold", HIGH_FLOW_THRESHOLD)
        );
    }
}
