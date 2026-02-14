package com.ncwu.iotservice.service.impl;

import com.ncwu.common.apis.iot_device.VirtualMeterDeviceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.ncwu.common.enums.ErrorCode;
import com.ncwu.common.enums.SuccessCode;
import com.ncwu.common.domain.vo.Result;
import com.ncwu.iotservice.entity.IotDeviceData;
import com.ncwu.iotservice.exception.QueryFailedException;
import com.ncwu.iotservice.mapper.IoTDeviceDataMapper;
import com.ncwu.iotservice.service.IoTDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    private final IoTDeviceDataMapper ioTDeviceDataMapper;

    @DubboReference(version = "1.0.0")
    private VirtualMeterDeviceService virtualMeterDeviceService;

    @Override
    public Result<Double> getRangeUsage(LocalDateTime start, LocalDateTime end, String deviceId) {
        DateFormatBo result = getDateFormatBo(start, end);
        // Flux 查询一次拿到 start 和 end 的累计值
        String fluxQuery = String.format("""
                from(bucket:"water")
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
                from(bucket: "water")
                |> range(start: -1m)
                |> filter(fn: (r) =>
                                r._measurement == "water_meter" and
                        r._field == "flow" and
                        r.deviceId == "%s"
                   )
                |> last()
                |> keep(columns: ["_time", "_value"])
                """, deviceId);

        Double flow;
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
                
                from(bucket: "water")
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

            return Result.ok(Double.NaN, SuccessCode.DATA_EMPTY.getCode(), SuccessCode.DATA_EMPTY.getMessage());
        }
        //计算环比
        double annulus = (todayUsage.getData() - yestDayUsage.getData()) / yestDayUsage.getData();
        return Result.ok(annulus);
    }

    @Override
    public Result<Double> getOfflineRate() {
        double offLineRate = ioTDeviceDataMapper.getOffLineRate();
        return Result.ok(offLineRate);
    }

    @Override
    public Result<Double> getWaterQuality(String deviceId) {
        Result<Double> turbidity = getTurbidity(deviceId);
        Result<Double> ph = getPh(deviceId);
        Result<Double> chlorine = getChlorine(deviceId);

        Double chlorineData = chlorine.getData();
        Double phData = ph.getData();
        Double turbidityData = turbidity.getData();

        if (chlorineData == null || phData == null || turbidityData == null) {
            return Result.fail(Double.NaN, ErrorCode.QUERY_FAILED_ERROR.code(), ErrorCode.QUERY_FAILED_ERROR.message());
        } else if (chlorineData.isNaN() || phData.isNaN() || turbidityData.isNaN()) {
            return Result.ok(Double.NaN);
        } else {
            if (turbidityData > 1.0 || (phData < 6.5 || phData > 8.5)
                    || (chlorineData < 0.05 || chlorineData > 0.85)) {
                //不合格
                return Result.ok(0.0);
            } else {
                String pythonExecutable = "python3";
                String pythonScriptPath = Objects.requireNonNull(getClass().getClassLoader()
                        .getResource("water_quality.py")).getPath();
                if(System.getProperty("os.name").toLowerCase().contains("windows")){
                    // 处理Windows路径中的URL编码
                    pythonScriptPath = pythonScriptPath.replace("/", "\\");
                    if (pythonScriptPath.startsWith("\\")) {
                        pythonScriptPath = pythonScriptPath.substring(1);
                    }
                }
                String[] cmd = {
                        pythonExecutable,
                        pythonScriptPath,
                        String.valueOf(turbidityData),
                        String.valueOf(phData),
                        String.valueOf(chlorineData)
                };
                System.out.println("Python命令: " + String.join(" ", cmd));
                ProcessBuilder processBuilder = new ProcessBuilder(cmd);
                try {
                    Process pr = processBuilder.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
                    String line;
                    StringBuilder output = new StringBuilder();
                    StringBuilder errorOutput = new StringBuilder();

                    // 读取标准输出
                    while ((line = reader.readLine()) != null) {
                        output.append(line);
                    }

                    // 读取错误输出
                    while ((line = errorReader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                    }

                    System.out.println("Python 标准输出: " + output);
                    if (!errorOutput.isEmpty()) {
                        System.out.println("Python 错误输出: " + errorOutput);
                    }
                    pr.waitFor();

                    String resultStr = output.toString().trim();
                    try {
                        double result = keep2(Double.parseDouble(resultStr)) * 100;
                        return Result.ok(result);
                    } catch (NumberFormatException e) {
                        System.out.println("无法解析Python输出为数字: " + resultStr);
                        return Result.fail(Double.NaN, ErrorCode.QUERY_FAILED_ERROR.code(), "Cannot parse Python output: " + resultStr);
                    }
                } catch (IOException | InterruptedException e) {
                    return Result.fail(Double.NaN, ErrorCode.QUERY_FAILED_ERROR.code(), ErrorCode.QUERY_FAILED_ERROR.message());
                }
            }
        }
    }

    @Override
    public Result<Double> getTurbidity(String deviceId) {
        String flux = String.format("""
                    from(bucket: "water")
                      |> range(start: -1m)
                      |> filter(fn: (r) =>
                        r._measurement == "water_quality" and
                        r._field == "turbidity" and
                        r.deviceId == "%s"
                      )
                      |> last()
                      |> keep(columns: ["_time", "_value"])
                """, deviceId);
        return getQueryResult(flux);
    }

    @NonNull
    private Result<Double> getQueryResult(String flux) {
        Double v = null;
        try {
            List<FluxTable> query = influxDBClient.getQueryApi().query(flux);
            v = query.stream().flatMap(t -> t.getRecords().stream())
                    .map(r -> r.getValue() != null ? ((Number) r.getValue()).doubleValue() : Double.NaN)
                    .findFirst()
                    .orElse(Double.NaN);
        } catch (Exception e) {
            throw new QueryFailedException("influxDB 查询失败");
        }
        return Result.ok(v);
    }

    @Override
    public Result<Double> getPh(String deviceId) {
        String flux = String.format("""
                    from(bucket: "water")
                      |> range(start: -1m)
                      |> filter(fn: (r) =>
                        r._measurement == "water_quality" and
                        r._field == "ph" and
                        r.deviceId == "%s"
                      )
                      |> last()
                      |> keep(columns: ["_time", "_value"])
                """, deviceId);
        return getQueryResult(flux);
    }

    @Override
    public Result<Double> getChlorine(String deviceId) {
        String flux = String.format("""
                    from(bucket: "water")
                      |> range(start: -1m)
                      |> filter(fn: (r) =>
                        r._measurement == "water_quality" and
                        r._field == "chlorine" and
                        r.deviceId == "%s"
                      )
                      |> last()
                      |> keep(columns: ["_time", "_value"])
                """, deviceId);
        return getQueryResult(flux);
    }

    @Override
    public Result<Map<LocalDateTime, Double>> getFlowTendency(
            LocalDateTime start,
            LocalDateTime end,
            String deviceId) {

        DateFormatBo dateFormatBo = getDateFormatBo(start, end);
        String startTime = dateFormatBo.startTime();
        String endTime = dateFormatBo.endTime();

        String flux = String.format("""
                from(bucket: "water")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) =>
                    r._measurement == "water_quality" and
                    r._field == "flow" and
                    r.deviceId == "%s"
                  )
                  |> keep(columns: ["_time", "_value"])
                """, startTime, endTime, deviceId);

        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux);

        // ① 先把 Stream 变成 List，避免二次消费
        List<FluxRecord> records = tables.stream()
                .flatMap(t -> t.getRecords().stream())
                .toList();

        // ② 组装时间 -> 值
        Map<LocalDateTime, Double> resultMap = records.stream()
                .collect(Collectors.toMap(
                        r -> LocalDateTime.ofInstant(
                                Objects.requireNonNull(r.getTime()),
                                ZoneId.systemDefault()
                        ),
                        r -> r.getValue() != null
                                ? ((Number) r.getValue()).doubleValue()
                                : 0.0,
                        (v1, v2) -> v2,              // 时间重复时取后者
                        LinkedHashMap::new           // 保留时间顺序
                ));
        return Result.ok(resultMap);
    }

    @Override
    public Result<Double> getHealthyScoreOfDevices() {
        //根据离线率，和异常事件的占比评价集群设备健康度
        Result<Double> offlineRate = getOfflineRate();
        Double offlineRateData = offlineRate.getData();
        // 获取总设备数
        Result<Integer> deviceNums = virtualMeterDeviceService.getDeviceNums();
        Integer nums = deviceNums.getData();
        if (nums == 0) {
            return Result.ok(0.0);
        }
        // 获取未处理的异常事件数
        int unhandledAbnormalEvents = ioTDeviceDataMapper.countUnhandledAbnormalEvents();

        // 计算异常事件率（每台设备的平均异常事件数，上限设为5）
        double abnormalEventRate = Math.min((double) unhandledAbnormalEvents / nums, 5.0);

        if (offlineRateData != null) {
            // 健康度评分计算：
            // 基础分100分
            // 离线率扣分：离线率 * 40分（离线率影响最大）
            // 异常事件扣分：异常事件率 * 12分（每台设备平均1个异常事件扣12分）
            double healthScore = 100.0 - (offlineRateData * 40.0) - (abnormalEventRate * 12.0);

            // 确保分数在0-100范围内
            healthScore = Math.max(0.0, Math.min(100.0, healthScore));

            return Result.ok(keep2(healthScore));
        }
        return Result.fail(Double.NaN, ErrorCode.GET_HEALTHY_SCORE_ERROR.code(), ErrorCode.GET_HEALTHY_SCORE_ERROR.message());
    }
}
