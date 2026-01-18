package com.ncwu.iotservice.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.ncwu.common.vo.Result;
import com.ncwu.iotservice.entity.IotDeviceData;
import com.ncwu.iotservice.mapper.IoTDeviceDataMapper;
import com.ncwu.iotservice.service.IoTDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

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
    public Result<Double> getRangeUsage(long start, long end,String deviceId) {
        // 转换时间戳为 RFC3339
        String startTime = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(start));
        String endTime = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(end));
        // Flux 查询一次拿到 start 和 end 的累计值
        String fluxQuery = String.format("""
                from(bucket:"test08")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) => r._measurement == "water_meter" and r._field == "usage" and r.deviceId == %s)
                  |> keep(columns: ["_time", "_value"])
                """, startTime, endTime,deviceId);

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
        return Result.ok(Double.valueOf(Objects.requireNonNull(redisTemplate.opsForHash()
                .get("meter:total_usage", deviceId)).toString()));
    }
}
