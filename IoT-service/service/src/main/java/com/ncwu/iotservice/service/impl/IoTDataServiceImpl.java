package com.ncwu.iotservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.AtomicDouble;
import com.ncwu.common.apis.iot_device.VirtualMeterDeviceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.ncwu.common.apis.iot_service.IotDataService;
import com.ncwu.common.enums.ErrorCode;
import com.ncwu.common.enums.SuccessCode;
import com.ncwu.common.domain.vo.Result;
import com.ncwu.iotservice.config.ServiceConfig;
import com.ncwu.iotservice.entity.BO.SchoolUsageBO;
import com.ncwu.common.domain.bo.ToAIBO;
import com.ncwu.iotservice.entity.IotDeviceData;
import com.ncwu.iotservice.entity.WaterUsageRecord;
import com.ncwu.iotservice.exception.QueryFailedException;
import com.ncwu.iotservice.mapper.IoTDeviceDataMapper;
import com.ncwu.iotservice.mapper.WaterUsageRecordMapper;
import com.ncwu.iotservice.service.IoTDataService;
import com.ncwu.iotservice.util.ExcelExportUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.jspecify.annotations.NonNull;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.ncwu.common.utils.Utils.keep2;

/**
 * 查询、计算Iot设备数据
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
@DubboService(version = "1.0.0", interfaceClass = IotDataService.class)
public class IoTDataServiceImpl extends ServiceImpl<IoTDeviceDataMapper, IotDeviceData> implements IoTDataService, IotDataService {

    private final InfluxDBClient influxDBClient;
    private final StringRedisTemplate redisTemplate;
    private final IoTDeviceDataMapper ioTDeviceDataMapper;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final ServiceConfig serviceConfig;

    private final WaterUsageRecordMapper waterUsageRecordMapper;
    private final ExecutorService pool = Executors.newFixedThreadPool(100);

    @DubboReference(version = "1.0.0")
    private VirtualMeterDeviceService virtualMeterDeviceService;

    @Override
    public Result<Double> getRangeUsage(LocalDateTime start, LocalDateTime end, String deviceId) {
        //todo 加入redis缓存
        // TTL [4,6]分钟 key自定(value结构即可)
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
        // InfluxDB Flux 要求 UTC
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
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
        //todo 加入redis缓存
        // TTL 不超过10秒 key自定(value结构即可)
        String fluxQuery = String.format("""
                from(bucket: "water")
                |> range(start: -10s)
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

    /**
     * 此方法的调用是昂贵的。必要时，需要分布式锁+逻辑过期
     */
    @Override
    public Result<Double> getSchoolUsage(int school, LocalDateTime start, LocalDateTime end) {
        //时间戳转换
        DateFormatBo dateFormatBo = getDateFormatBo(start, end);
        String startTime = dateFormatBo.startTime();
        String endTime = dateFormatBo.endTime();

        //检查时间跨度：超过23小时视为历史数据查询，使用分级缓存策略
        if (Duration.between(start, end).toHours() >= 23) {
            return getHistoricalSchoolUsageWithCache(school, startTime, endTime);
        }

        RLock lock = redissonClient.getLock("SchoolUsageUpdateLock" + school);
        Double res = null;
        String json = redisTemplate.opsForValue().get("SchoolUsage:" + school);
        if (json == null) {
            try {
                if (lock.tryLock()) {
                    Result<Double> usage = getSchoolUsageFromDb(school, startTime, endTime);
                    res = usage.getData();
                    setValueToCache(res, school);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } else {
            SchoolUsageBO usageBO;
            try {
                usageBO = objectMapper.readValue(json, SchoolUsageBO.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            if (usageBO.getExpireTime().isBefore(LocalDateTime.now())) {
                res = usageBO.getUsage();
                pool.submit(() -> {
                    try {
                        if (lock.tryLock()) {
                            Result<Double> usage = getSchoolUsageFromDb(school, startTime, endTime);
                            setValueToCache(usage.getData(), school);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                });
            } else {
                res = usageBO.getUsage();
            }
        }
        if (res == null) {
            return getSchoolUsageFromDb(school, startTime, endTime);
        }
        return Result.ok(res);
    }

    @Async
    public void setValueToCache(Double data, int school) {
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(30);
        SchoolUsageBO usageBO = new SchoolUsageBO(data, expireTime);
        try {
            //序列化
            String json = objectMapper.writeValueAsString(usageBO);
            redisTemplate.opsForValue().set("SchoolUsage:" + school, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Result<Double> getSchoolUsageFromDb(int school, String startTime, String endTime) {
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
        List<FluxTable> tables = influxDBClient.getQueryApi().query(fluxQuery);
        if (tables.isEmpty()) {
            return Result.ok(null);
        }
        usage = tables.stream()
                .flatMap(table -> table.getRecords().stream())
                .map(record -> (record.getValue() != null ? ((Number) record.getValue()).doubleValue() : null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        // 负数说明数据异常，返回null
        if (usage != null && usage < 0) {
            return Result.ok(null);
        }
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
    public Result<Double> getWaterQualityScore(String deviceId) {
        String score = redisTemplate.opsForValue().get("WaterQualityScore:" + deviceId);
        if (score != null) {
            return Result.ok(Double.parseDouble(score));
        } else {
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
                if (turbidityData > 1.0 || (phData < 6.5 || phData > 8.5) || (chlorineData < 0.05 || chlorineData > 0.85)) {
                    //不合格
                    return Result.ok(0.0);
                } else {
                    String pythonExecutable = "python3.12";
                    String pythonScriptPath = Objects.requireNonNull(getClass().getClassLoader()
                            .getResource("water_quality.py")).getPath();
                    if (System.getProperty("os.name").toLowerCase().contains("windows")) {
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
//                    System.out.println("Python命令: " + String.join(" ", cmd));
                    ProcessBuilder processBuilder = new ProcessBuilder(cmd);
                    try {
                        Process pr = processBuilder.start();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
//                        BufferedReader errorReader = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
                        String line;
                        StringBuilder output = new StringBuilder();
//                        StringBuilder errorOutput = new StringBuilder();
                        // 读取标准输出
                        while ((line = reader.readLine()) != null) {
                            output.append(line);
                        }
//                        // 读取错误输出
//                        while ((line = errorReader.readLine()) != null) {
//                            errorOutput.append(line).append("\n");
//                        }
//                        if (!errorOutput.isEmpty()) {
//                            log.error("Python 错误输出: {}", errorOutput);
//                        }
                        pr.waitFor();
                        String resultStr = output.toString().trim();
                        try {
                            double result = keep2(Double.parseDouble(resultStr)) * 100;
                            redisTemplate.opsForValue().set("WaterQualityScore:" + deviceId, String.valueOf(result)
                                    , 60 + ThreadLocalRandom.current().nextInt(10), TimeUnit.SECONDS);
                            return Result.ok(result);
                        } catch (NumberFormatException e) {
                            log.error("无法解析Python输出为数字: {}", resultStr);
                            return Result.fail(Double.NaN, ErrorCode.QUERY_FAILED_ERROR.code(),
                                    "Cannot parse Python output: " + resultStr);
                        }
                    } catch (IOException | InterruptedException e) {
                        return Result.fail(Double.NaN, ErrorCode.QUERY_FAILED_ERROR.code(), ErrorCode.QUERY_FAILED_ERROR.message());
                    }
                }
            }
        }

    }

    @Override
    public Result<Double> getTurbidity(String deviceId) {
        //todo redis缓存 TTL 不超过10秒，key自定义
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
        Double v;
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
        //todo redis缓存 TTL 不超过10秒，key自定义
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
        //todo redis缓存 TTL 不超过10秒，key自定义
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

    @Override
    public Result<Collection<String>> getOffLineList(int campus) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries("OnLineMap");
        Set<Object> ids = entries.keySet();

        Set<String> res = ids.stream()
                .map(Object::toString)
                .filter(id -> id.substring(1, 2)
                        //保留目标校区
                        .equals(String.valueOf(campus)) && id.startsWith("1"))
                .collect(Collectors.toSet());
        //全量列表
        Set<String> meters = redisTemplate.opsForSet().members("device:meter");
        assert meters != null;
        //移除其他校区的
        Set<String> collect = meters.stream()
                .filter(id -> id.substring(1, 2).equals(String.valueOf(campus)))
                .collect(Collectors.toSet());
        //移除在线列表
        collect.removeAll(res);
        return Result.ok(collect);
    }

    @Override
    public Result<Long> getCampusOnLineNum(int campus) {
        return Result.ok(redisTemplate.opsForHash()
                .entries("OnLineMap")
                .keySet()
                .stream()
                .map(Object::toString)
                .filter(id -> id.substring(1, 2).equals(String.valueOf(campus)) && id.startsWith("1")).count());
    }

    @Override
    public Result<Double> getPressureNow(String deviceId) {
        //todo 加入redis缓存
        // TTL 不超过10秒 key自定(value结构即可)
        String fluxQuery = String.format("""
                from(bucket: "water")
                |> range(start: -1m)
                |> filter(fn: (r) =>
                                r._measurement == "water_meter" and
                        r._field == "pressure" and
                        r.deviceId == "%s"
                   )
                |> last()
                |> keep(columns: ["_time", "_value"])
                """, deviceId);
        Double pressure;
        try {
            pressure = influxDBClient.getQueryApi().query(fluxQuery).stream()
                    .flatMap(t -> t.getRecords().stream())
                    .map(r -> r.getValue() != null ? ((Number) r.getValue()).doubleValue() : 0)
                    .findFirst()
                    .orElse(0.0);
        } catch (Exception e) {
            throw new QueryFailedException("查询失败，请重试");
        }
        return Result.ok(pressure);
    }

    @Override
    public Result<Double> getTemNow(String deviceId) {
        //todo 加入redis缓存
        // TTL 不超过10秒 key自定(value结构即可)
        String fluxQuery = String.format("""
                from(bucket: "water")
                |> range(start: -1m)
                |> filter(fn: (r) =>
                                r._measurement == "water_meter" and
                        r._field == "tem" and
                        r.deviceId == "%s"
                   )
                |> last()
                |> keep(columns: ["_time", "_value"])
                """, deviceId);
        Double tem;
        try {
            tem = influxDBClient.getQueryApi().query(fluxQuery).stream()
                    .flatMap(t -> t.getRecords().stream())
                    .map(r -> r.getValue() != null ? ((Number) r.getValue()).doubleValue() : 0)
                    .findFirst()
                    .orElse(0.0);
        } catch (Exception e) {
            throw new QueryFailedException("查询失败，请重试");
        }
        return Result.ok(tem);

    }

    @Override
    public Result<Double> getRate(int region, int campus) {
        //1 教学区
        //2 试验区
        //3 宿舍区
        AtomicDouble resOfEdu = new AtomicDouble(0.0);
        AtomicDouble resOfExp = new AtomicDouble(0.0);
        AtomicDouble resOfDom = new AtomicDouble(0.0);
        final int lastIndexOfEduBuilding = Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue()
                .get("device:educationBuildings")));

        final int lastIndexOfExpBuilding = Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue()
                .get("device:experimentBuildings")));

        redisTemplate.opsForHash().entries("meter:total_usage").forEach((k, v) -> {
            int index = Integer.parseInt(k.toString().substring(2, 4));
            if (index <= lastIndexOfEduBuilding) {
                resOfEdu.addAndGet(Double.parseDouble(v.toString()));
            } else if (index <= lastIndexOfExpBuilding) {
                resOfExp.addAndGet(Double.parseDouble(v.toString()));
            } else {
                resOfDom.addAndGet(Double.parseDouble(v.toString()));
            }
        });

        double sum = resOfEdu.get() + resOfExp.get() + resOfDom.get();
        if (region == 1) {
            return Result.ok(resOfEdu.get() / sum);
        } else if (region == 2) {
            return Result.ok(resOfExp.get() / sum);
        } else {
            return Result.ok(resOfDom.get() / sum);
        }

    }

    @Override
    public Result<List<LocalDateTime>> getHighWaterUsageTime(int campus) {
        // 查询过去一天内，每30分钟用水数据（48个时间段）
        LocalDateTime now = LocalDateTime.now();

        // 生成时间段列表
        List<LocalDateTime[]> timeSlots = new ArrayList<>();
        for (int i = 95; i >= 0; i--) {
            LocalDateTime start = now.minusMinutes((i + 1) * 15L);
            LocalDateTime end = now.minusMinutes(i * 30L);
            timeSlots.add(new LocalDateTime[]{start, end});
        }

        // 并行查询
        List<CompletableFuture<Map.Entry<Double, LocalDateTime>>> futures = timeSlots.stream()
                .map(slot -> CompletableFuture.supplyAsync(() -> {
                    String startStr = DateTimeFormatter.ISO_INSTANT.format(slot[0].atZone(ZoneId.systemDefault()).toInstant());
                    String endStr = DateTimeFormatter.ISO_INSTANT.format(slot[1].atZone(ZoneId.systemDefault()).toInstant());
                    try {
                        Result<Double> usage = getSchoolUsageFromDb(campus, startStr, endStr);
                        if (usage != null && usage.getData() != null) {
                            return new AbstractMap.SimpleEntry<>(usage.getData(), slot[0]);
                        }
                    } catch (Exception e) {
                        log.warn("查询用水量失败: {} - {}", slot[0], e.getMessage());
                    }
                    return (Map.Entry<Double, LocalDateTime>) null;
                }, pool))
                .toList();

        // 等待所有结果
        List<Map.Entry<Double, LocalDateTime>> usageList = futures
                .stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .sorted((a, b) ->
                        b.getKey().compareTo(a.getKey())).toList();

        // 按照用水量进行排序（降序）

        // 取前3个高峰时段
        List<LocalDateTime> highUsageTimes = new ArrayList<>();
        for (int i = 0; i < Math.min(3, usageList.size()); i++) {
            highUsageTimes.add(usageList.get(i).getValue());
        }

        if (highUsageTimes.isEmpty()) {
            return Result.ok(null);
        }

        return Result.ok(highUsageTimes);
    }

    @Override
    public Result<Map<Integer, Double>> getCampusRate() {
        //1 花园
        //2 龙子湖
        //3 江淮
        double[] res = new double[4];
        HashMap<Integer, Double> map = new HashMap<>();
        double sum = 0.0;
        CompletableFuture<Double>[] futures = new CompletableFuture[4];
        for (int i = 1; i <= 3; i++) {
            int finalI = i;
            futures[i] = CompletableFuture.supplyAsync(() -> getSchoolUsage(finalI, LocalDateTime.now().minusDays(1),
                    LocalDateTime.now()).getData());
        }
        for (int i = 1; i <= 3; i++) {
            res[i] = futures[i].join();
            sum += res[i];
        }
        for (int i = 1; i <= 3; i++) {
            if (sum == 0) {
                map.put(i, Double.NaN);
            } else {
                map.put(i, res[i] / sum);
            }

        }
        return Result.ok(map);
    }

    @Override
    public Result<ToAIBO> getRecentWeekUsage() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);

        List<Double> HY = new ArrayList<>();
        List<Double> LH = new ArrayList<>();
        List<Double> JH = new ArrayList<>();

        // 查询三个校区的数据
        for (int school = 1; school <= 3; school++) {
            List<WaterUsageRecord> records = waterUsageRecordMapper.selectRecentRecords(school, startDate);
            List<Double> usageList = records.stream()
                    .map(WaterUsageRecord::getUsage)
                    .collect(Collectors.toList());

            if (school == 1) {
                HY = usageList;
            } else if (school == 2) {
                LH = usageList;
            } else {
                JH = usageList;
            }
        }

        ToAIBO toAIBO = new ToAIBO();
        toAIBO.setHY(HY);
        toAIBO.setLH(LH);
        toAIBO.setJH(JH);
        return Result.ok(toAIBO);
    }

    @Override
    public Result<Double> getQualityRate() {
        Set<String> members = redisTemplate.opsForSet().members("device:sensor");
        AtomicInteger cnt = new AtomicInteger(0);
        assert members != null;
        double n = members.size();
        members.forEach(id -> {
            if (getWaterQualityScore(id).getData() <= 60) {
                cnt.getAndAdd(1);
            }
        });
        if (cnt.get() == 0) {
            return Result.ok(1.0);
        } else {
            return Result.ok(1 - cnt.get() / n);
        }
    }

    @Override
    public Result<Map<String, Double>> getSwings() {
        LocalDateTime now = LocalDateTime.now();
        //结果映射集，每个学校的用水波动指数
        Map<String, Double> result = new HashMap<>();
        for (int i = 1; i <= 3; i++) {
            List<Double> points = new ArrayList<>();
            //采集次数
            for (int j = 15; j >= 1; j--) {
                // 从旧到新，保证时序正确
                DateFormatBo dateFormatBo = getDateFormatBo(
                        //采集粒度是40秒
                        now.minusSeconds((long) j * 40),
                        now.minusSeconds((long) (j - 1) * 40)
                );
                Double usage = getSchoolUsageFromDb(i, dateFormatBo.startTime, dateFormatBo.endTime).getData();
                if (usage == null) {
                    return Result.ok(Collections.emptyMap(), "200", "设备开启时间较短，暂无法分析");
                }
                double data = keep2(usage);
                if (data != 0) {
                    points.add(data);
                }

            }
            System.out.println(points);
            double index = calcFluctuationIndex(points.toArray(new Double[0]));
            result.put("school_" + i, index);
        }
        return Result.ok(result);
    }

    @Override
    public Result<Double> getUnNormalUsage(int campus) {
        double res = 0;
        // 从当天晚上22点整开始
        LocalDateTime night = LocalDateTime.now()
                .withHour(22)
                .withMinute(0)
                .withSecond(0);
        //从当天的晚上10点开始，到早上5点，每十分钟查询一次数据库。
        //如果得到的用水量超过阈值，则认为异常
        for (int j = 8 * 60 / 10; j >= 10; j -= 10) {
            LocalDateTime start = night.minusMinutes(j * 10L);
            LocalDateTime end = night.minusMinutes((j - 1) * 10L);
            //转换为influxdb要求的格式
            DateFormatBo dateFormatBo = getDateFormatBo(start, end);
            Result<Double> usage = getSchoolUsageFromDb(campus, dateFormatBo.startTime, dateFormatBo.endTime);
            Double data = usage.getData();
            data = data == null ? 0.00 : data;
            if (data >= 1000) {
                res += data;
            }
        }
        return Result.ok(res);
    }

    @Override
    public ResponseEntity<byte[]> getDeviceDatas(String deviceCode) {
        int maxSize = serviceConfig.getMaxSize();
        try {
            // 校验设备编号
            if (deviceCode == null || deviceCode.trim().isEmpty()) {
                throw new IllegalArgumentException("设备编号不能为空");
            }

            // 查询设备数据
            LambdaQueryWrapper<IotDeviceData> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(IotDeviceData::getDeviceCode, deviceCode)
                    .orderByDesc(IotDeviceData::getCollectTime);
            
            // 使用分页查询，避免SQL注入
            int safeSize = Math.max(1, Math.min(maxSize, 10000)); // 安全限制：1-10000条
            Page<IotDeviceData> page = new Page<>(1, safeSize);
            IPage<IotDeviceData> deviceDataPage = baseMapper.selectPage(page, queryWrapper);
            List<IotDeviceData> deviceDataList = deviceDataPage.getRecords();

            if (deviceDataList.isEmpty()) {
                throw new IllegalArgumentException("未找到设备数据");
            }

            // 转换为Map列表用于Excel导出
            List<Map<String, Object>> dataList = deviceDataList.stream()
                    .map(data -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("deviceCode", data.getDeviceCode());
                        map.put("deviceType", data.getDeviceType());
                        map.put("collectTime", data.getCollectTime());
                        map.put("dataPayload", data.getDataPayload());
                        map.put("createTime", data.getCreateTime());
                        return map;
                    })
                    .collect(Collectors.toList());

            // 使用工具类导出Excel
            return ExcelExportUtil.exportDeviceDataToExcel(deviceCode, dataList);

        } catch (Exception e) {
            log.error("导出设备数据报表失败: {}", e.getMessage(), e);
            throw new RuntimeException("导出设备数据报表失败: " + e.getMessage());
        }
    }

    /**
     * 环比波动均值
     * <p>
     * index = mean(|x[t] - x[t-1]| / x[t-1]) * 100
     */
    private double calcFluctuationIndex(Double... values) {
        if (values == null || values.length < 2) return 0.0;

        double sum = 0.0;
        int count = 0;

        for (int i = 1; i < values.length; i++) {
            Double prev = values[i - 1];
            Double curr = values[i];
            if (prev == null || curr == null || prev == 0) continue;
            sum += Math.abs(curr - prev) / prev;
            count++;
        }

        return count == 0 ? 0.0 : (sum / count) * 100;
    }

    /**
     * 获取历史校区用水量（带缓存）
     * 分级缓存策略：历史数据使用独立缓存，TTL为2小时
     */
    private Result<Double> getHistoricalSchoolUsageWithCache(int school, String startTime, String endTime) {
        String cacheKey = buildHistoricalCacheKey(school, startTime, endTime);

        // 先尝试从缓存获取
        String cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            try {
                return Result.ok(Double.valueOf(cachedData));
            } catch (NumberFormatException e) {
                log.warn("缓存数据格式异常，key: {}, value: {}", cacheKey, cachedData);
                // 删除异常缓存数据
                redisTemplate.delete(cacheKey);
            }
        }

        // 缓存未命中，使用分布式锁防止缓存击穿
        RLock lock = redissonClient.getLock("historical:usage:lock:" + school);
        try {
            if (lock.tryLock(5, TimeUnit.SECONDS)) {
                // 双重检查，防止其他线程已经设置了缓存
                cachedData = redisTemplate.opsForValue().get(cacheKey);
                if (cachedData != null) {
                    return Result.ok(Double.valueOf(cachedData));
                }
                // 查询数据库并缓存结果
                Result<Double> dbResult = getSchoolUsageFromDb(school, startTime, endTime);
                redisTemplate.opsForValue().set(cacheKey, String.valueOf(dbResult.getData()), Duration.ofHours(2));
                return dbResult;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断, school: {}", school);
        } catch (Exception e) {
            log.error("查询历史用水量异常, school: {}", school, e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        // 锁获取失败时，返回NaN（避免数据库压力）
        log.warn("获取历史用水量锁失败，返回NaN，school: {}", school);
        return Result.ok(Double.NaN);
    }

    /**
     * 构建历史数据缓存键
     */
    private String buildHistoricalCacheKey(int school, String startTime, String endTime) {
        return String.format("historical:usage:%d:%s:%s", school,
                startTime.replaceAll("[:.]", "-"),
                endTime.replaceAll("[:.]", "-"));
    }
}
