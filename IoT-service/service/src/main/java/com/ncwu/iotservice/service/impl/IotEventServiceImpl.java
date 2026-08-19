package com.ncwu.iotservice.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxTable;
import com.ncwu.common.domain.vo.Result;
import com.ncwu.common.domain.IotDeviceEvent;
import com.ncwu.iotservice.mapper.IoTDeviceEventMapper;
import com.ncwu.iotservice.service.IoTEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IotEventServiceImpl extends ServiceImpl<IoTDeviceEventMapper, IotDeviceEvent> implements IoTEventService {

    private final InfluxDBClient influxDBClient;
    private final StringRedisTemplate redisTemplate;

    private QueryApi queryApi;

    private static final double LEAK_FLOW_THRESHOLD = 0.1;
    private static final char WATER_METER_TYPE = '1';

    @Override
    public Result<List<List<String>>> getLeakingDeviceList() {
        LocalDateTime now = LocalDateTime.now();
        if (now.getHour() == 23 || now.getHour() <= 5) {
            return check();
        } else {
            return Result.ok("200", "不在目标时段，暂无法检测");
        }
    }

    private Result<List<List<String>>> check() {
        List<List<String>> list = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            List<String> res = run(i);
            list.add(res);
        }
        return Result.ok(list);
    }

    private List<String> run(int campus) {
        Set<Object> ids = redisTemplate.opsForHash().entries("OnLineMap").keySet();
        StringBuilder deviceFilter = new StringBuilder();
        for (Object id : ids) {
            String s = id.toString();
            if (s.length() >= 2 && s.charAt(0) == WATER_METER_TYPE && s.substring(1, 2).equals(String.valueOf(campus))) {
                if (deviceFilter.length() > 0) {
                    deviceFilter.append(" or ");
                }
                deviceFilter.append("r.deviceId == \"").append(s).append("\"");
            }
        }
        if (deviceFilter.isEmpty()) {
            return List.of();
        }
        String flux = String.format("""
            from(bucket: "water")
              |> range(start: -30s)
              |> filter(fn: (r) =>
                r._measurement == "water_meter" and
                r._field == "flow" and
                (%s)
              )
            """, deviceFilter);
        List<String> leakingDevices = new ArrayList<>();
        try {
            for (FluxTable table : queryApi.query(flux)) {
                for (var record : table.getRecords()) {
                    Double value = record.getValue() != null ? ((Number) record.getValue()).doubleValue() : 0;
                    if (value > LEAK_FLOW_THRESHOLD) {
                        Object deviceId = record.getValueByKey("deviceId");
                        if (deviceId != null) {
                            leakingDevices.add(String.valueOf(deviceId));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("漏水检测异常: {}", e.getMessage());
        }
        return leakingDevices;
    }
}
