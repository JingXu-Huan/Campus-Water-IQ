package com.ncwu.iotservice.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.ncwu.common.enums.ErrorCode;
import com.ncwu.common.enums.SuccessCode;
import com.ncwu.common.vo.Result;
import com.ncwu.iotservice.entity.IotDeviceData;
import com.ncwu.iotservice.exception.QueryFailedException;
import com.ncwu.iotservice.mapper.IoTDeviceDataMapper;
import com.ncwu.iotservice.service.IoTDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.*;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class IoTDataServiceImpl extends ServiceImpl<IoTDeviceDataMapper, IotDeviceData> implements IoTDataService {

    private final InfluxDBClient influxDBClient;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Result<Double> getRangeUsage(LocalDateTime start, LocalDateTime end, String deviceId) {
        DateFormatBo result = getDateFormatBo(start, end);
        // Flux 查询一次拿到 start 和 end 的累计值
        String fluxQuery = String.format("""
                from(bucket:"test08")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) => r._measurement == "water_meter" and r._field == "usage" and r.deviceId == "%s")
                  |> keep(columns: ["_time", "_value"])
                  |> sort(columns:["_time"])
                """, result.startTime(), result.endTime(), deviceId);

        QueryApi queryApi = influxDBClient.getQueryApi();

        List<Double> values;
        try {
            values = queryApi.query(fluxQuery).stream()
                    .flatMap(t -> t.getRecords().stream())
                    .map(r -> r.getValue() != null ? ((Number) r.getValue()).doubleValue() : 0)
                    .toList();
        } catch (Exception e) {
            throw new QueryFailedException(null);
        }

        if (values.isEmpty()) {
            return Result.ok(SuccessCode.DATA_EMPTY.getCode(), SuccessCode.DATA_EMPTY.getMessage());
        }

        double startValue = values.getFirst(); // 第一条 = start 时间点累计量
        double endValue = values.getLast(); // 最后一条 = end 时间点累计量
        double usage = endValue - startValue;
        return Result.ok(keep2(usage));
    }

    /**
     * 将本地时区格式转化成 UTC 格式
     */
    private static @NonNull DateFormatBo getDateFormatBo(LocalDateTime start, LocalDateTime end) {
        ZoneId zoneId = ZoneId.of("Asia/Shanghai"); // InfluxDB Flux 要求 UTC
        Instant startInstant = start.atZone(zoneId).toInstant();
        Instant endInstant = end.atZone(zoneId).toInstant();
        // 转换时间戳为 RFC3339
        String startTime = DateTimeFormatter.ISO_INSTANT.format(startInstant);
        String endTime = DateTimeFormatter.ISO_INSTANT.format(endInstant);
        return new DateFormatBo(startTime, endTime);
    }

    private record DateFormatBo(String startTime, String endTime) {
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

        Double flow = null;
        try {
            flow = influxDBClient.getQueryApi().query(fluxQuery).stream()
                    .flatMap(t -> t.getRecords().stream())
                    .map(r -> r.getValue() != null ? ((Number) r.getValue()).doubleValue() : 0)
                    .findFirst()
                    .orElse(0.0);
        } catch (Exception e) {
            throw new QueryFailedException("查询失败，请重试");
        }

        return Result.ok(flow);
    }

    @Override
    public Result<Double> getSchoolUsage(int school, LocalDateTime start, LocalDateTime end) {
        //时间戳转换
        DateFormatBo dateFormatBo = getDateFormatBo(start, end);

        String startTime = dateFormatBo.startTime();
        String endTime = dateFormatBo.endTime();

        String fluxQuery = String.format("""
                import "strings"
                
                from(bucket: "test08")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) =>
                      r._measurement == "water_meter" and
                      r._field == "usage"
                  )
                  |> group(columns: ["deviceId"])
                  |> sort(columns: ["_time"])
                  |> reduce(
                      identity: {first: 0.0, last: 0.0, isFirst: true},
                      fn: (r, accumulator) => ({
                          first: if accumulator.isFirst then r._value else accumulator.first,
                          last: r._value,
                          isFirst: false
                      })
                  )
                  |> map(fn: (r) => ({
                      deviceId: r.deviceId,
                      usage: r.last - r.first
                  }))
                  |> filter(fn: (r) =>
                      strings.substring(v: r.deviceId, start: 1, end: 2) == "%s"
                  )
                  |> group()
                  |> sum(column: "usage")
                  |> map(fn: (r) => ({ _value: r.usage }))
                """, startTime, endTime, school);


        Double usage;

        usage = influxDBClient.getQueryApi().query(fluxQuery)
                .stream()
                .flatMap(table -> table.getRecords().stream())
                .map(record -> (record.getValue() != null ? ((Number) record.getValue()).doubleValue() : 0.0))
                .findFirst()
                .orElse(0.0);
        return Result.ok(usage);
    }

    @Override
    public Result<Double> getAnnulus(String deviceId) {
        LocalDateTime todayAtThisTime = LocalDateTime.now();
        LocalDateTime yestDayZeroTime = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime yestDayAtThisTime = todayAtThisTime.minusDays(1);
        LocalDateTime todayZeroTime = yestDayZeroTime.plusDays(1);


        Result<Double> yestDayUsage = getRangeUsage(yestDayZeroTime, yestDayAtThisTime, deviceId);
        Result<Double> todayUsage = getRangeUsage(todayZeroTime, todayAtThisTime, deviceId);

        //检查是否失败
        if (Objects.equals(yestDayUsage.getCode(), ErrorCode.QUERY_FAILED_ERROR.code()) ||
                Objects.equals(todayUsage.getCode(), ErrorCode.QUERY_FAILED_ERROR.code())) {
            return Result.fail(ErrorCode.QUERY_FAILED_ERROR.code(), ErrorCode.QUERY_FAILED_ERROR.message());
        }

        //检查是否有有效数据
        if (Objects.equals(yestDayUsage.getCode(), SuccessCode.DATA_EMPTY.getCode()) ||
                Objects.equals(todayUsage.getCode(), SuccessCode.DATA_EMPTY.getCode()) || yestDayUsage.getData() <= 0) {

            return Result.ok(Double.NaN,SuccessCode.DATA_EMPTY.getCode(), SuccessCode.DATA_EMPTY.getMessage());
        }
        //计算环比
        double annulus = (todayUsage.getData() - yestDayUsage.getData()) / yestDayUsage.getData();
        return Result.ok(annulus);
    }
}
