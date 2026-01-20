package com.ncwu.iotservice.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.ncwu.common.vo.Result;
import com.ncwu.iotservice.entity.IotDeviceData;
import com.ncwu.iotservice.mapper.IoTDeviceDataMapper;
import com.ncwu.iotservice.service.IoTDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.ncwu.common.utils.Utils.keep2;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/20
 */
@Service
@RequiredArgsConstructor
public class IoTDataServiceImpl extends ServiceImpl<IoTDeviceDataMapper, IotDeviceData> implements IoTDataService {

    private final InfluxDBClient influxDBClient;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Result<Double> getRangeUsage(LocalDateTime start, LocalDateTime end, String deviceId) {
        ZoneId zoneId = ZoneOffset.UTC; // InfluxDB Flux 要求 UTC
        Instant startInstant = start.atZone(zoneId).toInstant();
        Instant endInstant = end.atZone(zoneId).toInstant();
        // 转换时间戳为 RFC3339
        String startTime = DateTimeFormatter.ISO_INSTANT.format(startInstant);
        String endTime = DateTimeFormatter.ISO_INSTANT.format(endInstant);
        // Flux 查询一次拿到 start 和 end 的累计值
        String fluxQuery = String.format("""
                from(bucket:"test08")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) => r._measurement == "water_meter" and r._field == "usage" and r.deviceId == "%s")
                  |> keep(columns: ["_time", "_value"])
                """, startTime, endTime, deviceId);

        QueryApi queryApi = influxDBClient.getQueryApi();

        List<Double> values = queryApi.query(fluxQuery).stream()
                .flatMap(t -> t.getRecords().stream())
                .map(record -> record.getValue() != null ? ((Number) record.getValue()).doubleValue() : 0)
                .sorted() // 按时间升序
                .toList();

        if (values.isEmpty()) {
            return Result.fail("Data_1002", "时间段内没有数据");
        }

        double startValue = values.getFirst(); // 第一条 = start 时间点累计量
        double endValue = values.getLast(); // 最后一条 = end 时间点累计量
        double usage = endValue - startValue;
        return Result.ok(keep2(usage));
    }

    @Override
    public Result<Double> getTotalUsage(String deviceId) {
        Double value = Double.valueOf(Objects.requireNonNull(redisTemplate.opsForHash()
                .get("meter:total_usage", deviceId)).toString());
        return Result.ok(value);
    }

    @Override
    public Result<Map<String, Double>> getSumWaterUsage(List<String> ids) {
        List<Object> values = redisTemplate.opsForHash().multiGet("meter:total_usage", new ArrayList<>(ids));
        Map<String, Double> collect = IntStream.range(0, ids.size()).boxed()
                .collect(Collectors.toMap(ids::get, i -> Double.valueOf(values.get(i).toString())));
        return Result.ok(collect);
    }

    @Override
    public Result<Double> getFlowNow(String deviceId) {
        String fluxQuery = String.format("""
                from(bucket: "test08")
                |> range(start: -1m)
                |> filter(fn: (r) =>
                                r._measurement == "water_meter" and
                        r._field == "flow" and
                        r.deviceId == "%s"
                   )
                |> last()
                |> keep(columns: ["_time", "_value"])
                """, deviceId);
        List<Double> list = influxDBClient.getQueryApi().query(fluxQuery).stream().flatMap(t -> t.getRecords().stream())
                .map(record -> record.getValue() != null ? ((Number) record.getValue()).doubleValue() : 0).toList();
        if (list.isEmpty()) {
            return Result.ok(0.0);
        }
        return Result.ok(list.getFirst());

    }
}
