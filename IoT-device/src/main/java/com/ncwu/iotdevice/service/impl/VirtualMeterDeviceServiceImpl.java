package com.ncwu.iotdevice.service.impl;


import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ncwu.common.domain.vo.Result;
import com.ncwu.common.enums.ErrorCode;
import com.ncwu.common.enums.SuccessCode;
import com.ncwu.iotdevice.AOP.annotation.InitLuaScript;
import com.ncwu.iotdevice.AOP.annotation.Time;
import com.ncwu.common.Constants.DeviceStatus;
import com.ncwu.iotdevice.config.ServerConfig;
import com.ncwu.iotdevice.domain.Bo.DeviceIdList;
import com.ncwu.iotdevice.domain.Bo.MeterDataBo;
import com.ncwu.iotdevice.domain.entity.VirtualDevice;
import com.ncwu.iotdevice.mapper.DeviceMapper;
import com.ncwu.iotdevice.service.DataSender;
import org.springframework.beans.factory.ObjectProvider;
import com.ncwu.iotdevice.service.VirtualMeterDeviceService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.ncwu.common.Constants.DeviceStatus.UNKNOWN_START_ALL_DEVICE;
import static com.ncwu.iotdevice.AOP.Aspects.InitLuaScript.Lua_script;
import static com.ncwu.iotdevice.utils.Utils.*;

/**
 * 虚拟设备核心实现类
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/21
 */

@Slf4j
@Service
@DubboService(interfaceClass = com.ncwu.common.apis.iot_device.VirtualMeterDeviceService.class, version = "1.0.0")
@RequiredArgsConstructor
public class VirtualMeterDeviceServiceImpl extends ServiceImpl<DeviceMapper, VirtualDevice>
        implements VirtualMeterDeviceService, com.ncwu.common.apis.iot_device.VirtualMeterDeviceService {

    /**
     * 设备总数
     */
    private int allSize;
    /**
     * 运行中的设备集合，使用线程安全的ConcurrentHashMap
     */
    private final Set<String> runningDevices = ConcurrentHashMap.newKeySet();
    /**
     * Redis中设备状态的键前缀
     */
    final String deviceStatusPrefix = "cache:device:status:";

    /**
     * 调度器，用于管理定时任务
     */
    private ScheduledExecutorService scheduler;
    /**
     * Redis字符串模板，用于缓存操作
     */
    private final StringRedisTemplate redisTemplate;
    /**
     * RocketMQ消息模板，用于异步消息发送
     */
    private final RocketMQTemplate rocketMQTemplate;
    /**
     * 设备数据访问层
     */
    private final DeviceMapper deviceMapper;
    /**
     * 水质设备服务实现
     */
    private final VirtualWaterQualityDeviceServiceImpl waterQualityDeviceService;
    /**
     * 服务器配置
     */
    private final ServerConfig serverConfig;
    /**
     * 数据发送器，负责发送设备数据
     */
    @Autowired
    private ObjectProvider<DataSender> dataSender;
    /**
     * Redisson客户端，用于分布式锁等操作
     */
    private final RedissonClient redissonClient;
    /**
     * 本地缓存，使用Caffeine实现
     */
    Cache<String, String> cache;


    /**
     * 初始化方法，在Bean创建后自动调用
     * 创建调度器和本地缓存
     */
    @PostConstruct
    void init() {
        // 经压测验证，约 70 台虚拟设备对应 1 个调度线程即可满足精度与吞吐
        // 取 max 防止入参为 0 发生异常
        scheduler = Executors.newScheduledThreadPool(Math.max(5, (int) (this.count() / 70)));
        //初始化Caffeine本地缓存，最大容量10000，写入后5分钟过期
        cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    /**
     * 随机数生成器，用于生成随机延迟和数值
     */
    private static final Random RANDOM = new Random();
    /**
     * 线程池，用于执行异步任务，核心线程10，最大15，队列容量60，线程空闲时间60秒
     */
    final ExecutorService pool = getExecutorPools("iot-device", 10, 15, 60, 1000);
    /**
     * 存储每个设备的调度句柄，用于精准停止模拟
     */
    private final Map<String, ScheduledFuture<?>> deviceTasks = new ConcurrentHashMap<>();

    /**
     * 存储设备上报时间戳，以便和心跳对齐
     */
    private final ConcurrentHashMap<String, Long> reportTime = new ConcurrentHashMap<>();
    /**
     * 设备是否已经完成了初始化，使用volatile保证可见性
     */
    public volatile boolean isInit = false;

    /**
     * 应用销毁时调用，优雅关闭所有资源
     * 停止所有模拟任务，清理缓存和数据库数据
     */
    @PreDestroy
    public void destroy() {
        // 确保应用关闭时停止所有调度
        stopSimulation();
        // 确保应用关闭之后清空 redis 中所有数据
        clearRedisAndDbData(redisTemplate, deviceMapper);
        // 关闭调度器
        scheduler.shutdown();
    }

    /**
     * 启动所有虚拟水表设备模拟
     * <p>
     * 执行流程：<p>
     * 1. 检查设备初始化状态
     * <p>
     * 2. 获取所有设备ID并检查运行状态
     * <p>
     * 3. 批量更新数据库中的设备运行状态
     * <p>
     * 4. 使用Lua脚本更新Redis在线设备映射
     * <p>
     * 5. 为每个设备启动数据上报和心跳任务
     * <p>
     * 6. 清理相关缓存
     *
     * @return 启动结果，包含成功启动的设备数量
     */
    @Time  // AOP注解：记录方法执行耗时
    @InitLuaScript  // AOP注解：初始化Lua脚本
    @Override
    public Result<String> start() {
        // 检查设备初始化状态开关
        if (!isInit) {
            return Result.fail(ErrorCode.DEVICE_INIT_ERROR.code(),
                    ErrorCode.DEVICE_INIT_ERROR.message());
        }

        // 从Redis获取所有水表设备ID
        Set<String> readiedIds = redisTemplate.opsForSet().members("device:meter");

        // 检查模拟器状态：如果所有设备都已运行，返回错误
        if (readiedIds != null && runningDevices.size() == readiedIds.size()) {
            log.info("所有模拟设备已全部在运行中");
            return Result.fail(ErrorCode.DEVICE_DEVICE_RUNNING_NOW_ERROR.code(),
                    ErrorCode.DEVICE_DEVICE_RUNNING_NOW_ERROR.message());
        }
        int alreadyRunning = runningDevices.size();
        // 将设备ID添加到运行集合
        if (readiedIds != null) {
            runningDevices.addAll(readiedIds);
        }

        if (readiedIds != null && !readiedIds.isEmpty()) {
            // 异步更新数据库：将未运行的设备状态设置为运行中
            this.lambdaUpdate().in(VirtualDevice::getDeviceCode, readiedIds)
                    .set(VirtualDevice::getIsRunning, true).update();
            // 使用Lua脚本批量更新Redis在线设备映射，提高性能
            String hashKey = "OnLineMap";
            redisTemplate.execute(Lua_script, List.of(hashKey), "-1");

            // 为每个设备启动独立的数据上报和心跳任务
            runningDevices.forEach(this::scheduleNextReport);
            runningDevices.forEach(this::startHeartbeat);
            log.info("成功开启 {} 台设备的模拟数据流", readiedIds.size());

            // 设置设备可检查状态标志
            redisTemplate.opsForValue().set("MeterChecked", "1");

            // 清理相关缓存，确保数据一致性
            cache.invalidateAll();  // 清空本地缓存
            redisScanDel(deviceStatusPrefix + "*", 100, redisTemplate);  // 删除Redis设备状态缓存

            return Result.ok("成功开启" + (readiedIds.size() - alreadyRunning) + "台设备");
        }

        return Result.fail(UNKNOWN_START_ALL_DEVICE, ErrorCode.UNKNOWN.code(), ErrorCode.UNKNOWN.message());
    }

    /**
     * 批量启动指定的虚拟水表设备
     *
     * @param ids 需要启动的设备ID列表
     * @return 启动结果，包含成功启动的设备数量
     */
    @Override
    public Result<String> startList(List<String> ids) {
        // 检查是否所有设备都已运行
        if (ids != null && runningDevices.size() == allSize) {
            log.info("模拟器已全部在运行中，无需继续开启设备");
            return Result.fail(ErrorCode.DEVICE_DEVICE_RUNNING_NOW_ERROR.code(),
                    ErrorCode.DEVICE_DEVICE_RUNNING_NOW_ERROR.message());
        }

        // 检查设备初始化状态
        if (!isInit) {
            return Result.fail(ErrorCode.DEVICE_INIT_ERROR.code(),
                    ErrorCode.DEVICE_INIT_ERROR.message());
        }

        if (ids != null && !ids.isEmpty()) {
            List<String> keys = ids.stream().map(id -> deviceStatusPrefix + id).toList();
            // 删除指定设备的缓存
            redisTemplate.delete(keys);
            // 将设备添加到运行集合
            runningDevices.addAll(ids);
            // 更新Redis在线设备映射，记录上线时间
            String now = String.valueOf(System.currentTimeMillis());
            ids.forEach(id -> redisTemplate.opsForHash()
                    .put("OnLineMap", id, now));

            // 为每个设备启动心跳任务
            ids.forEach(this::startHeartbeat);

            // 为每个设备启动数据上报任务
            ids.forEach(this::scheduleNextReport);

            // 异步更新数据库：设置设备为运行状态和在线状态
            pool.submit(() -> {
                this.lambdaUpdate().in(VirtualDevice::getDeviceCode, ids)
                        .eq(VirtualDevice::getIsRunning, false)
                        .set(VirtualDevice::getIsRunning, true)
                        .set(VirtualDevice::getStatus, true).update();
            });

            log.info("批量：成功开启 {} 台设备的模拟数据流", ids.size());
            return Result.ok("成功开启" + ids.size() + "台设备");
        }

        return Result.fail(ErrorCode.UNKNOWN.code(), ErrorCode.UNKNOWN.message());
    }

    /**
     * 停止所有虚拟设备的模拟
     * <p>
     * 执行流程：<p>
     * 1. 设置设备不可检查状态<p>
     * 2. 清空运行设备集合<p>
     * 3. 取消所有定时任务<p>
     * 4. 通过消息队列异步更新数据库<p>
     * 5. 清理缓存
     *
     * @return 停止结果
     */
    @Time  // AOP注解：记录方法执行耗时
    @InitLuaScript  // AOP注解：初始化Lua脚本
    @Override
    public Result<String> stopSimulation() {
        // 设置设备不可检查状态
        redisTemplate.opsForValue().set("MeterChecked", "0");

        // 清空运行设备集合，停止所有设备
        runningDevices.clear();

        // 取消所有排队中的定时任务
        deviceTasks.forEach((id, future) -> {
            if (future != null && !future.isCancelled()) {
                future.cancel(false);  // 非强制取消，允许当前任务完成
            }
        });

        // 通过消息队列异步更新数据库状态
        // 由于异步线程的异常不被事务控制，使用消息队列确保可靠性
        rocketMQTemplate.convertAndSend("OpsForDataBase", "LetAllMetersStopRunning");

        // 清理状态缓存，使用scan安全删除
        cache.invalidateAll();  // 清空本地缓存
        redisScanDel(deviceStatusPrefix + "*", 100, redisTemplate);  // 删除Redis设备状态缓存

        // 清空任务映射
        deviceTasks.clear();

        log.info("已停止所有模拟数据上报任务");
        return Result.ok("已停止所有模拟数据上报任务");
    }

    /**
     * 停止单个或批量指定的虚拟设备模拟
     *
     * @param ids 需要停止的设备ID列表
     * @return 停止结果
     */
    @Override
    public Result<String> listStopSimulation(List<String> ids) {
        // 参数校验：设备列表不能为空
        if (ids == null || ids.isEmpty()) {
            return Result.fail(null, "设备列表为空");
        }

        // 遍历需要停止的设备，执行停止操作
        ids.forEach(id -> {
            ScheduledFuture<?> future = deviceTasks.get(id);
            if (future != null) {
                // 取消该设备的定时任务
                future.cancel(false);
                // 从任务映射中移除
                deviceTasks.remove(id);
                // 从运行设备集合中移除
                runningDevices.remove(id);
            }
        });

        // 异步更新数据库：将指定设备设置为非运行状态
        pool.submit(() -> {
            LambdaUpdateWrapper<VirtualDevice> running = new LambdaUpdateWrapper<VirtualDevice>()
                    .in(VirtualDevice::getDeviceCode, ids)
                    .eq(VirtualDevice::getIsRunning, true)
                    .set(VirtualDevice::getIsRunning, false);
            deviceMapper.update(running);
        });

        // 清理相关缓存
        cache.invalidateAll(ids);  // 清空本地缓存中指定设备
        ids.forEach(id -> redisTemplate.delete(deviceStatusPrefix + id));  // 删除Redis中的设备状态缓存

        return Result.ok(SuccessCode.DEVICE_STOP_SUCCESS.getCode(),
                SuccessCode.DEVICE_STOP_SUCCESS.getMessage());
    }

    /**
     * 查看设备运行状态
     */
    @Override
    public Result<Map<String, String>> checkDeviceStatus(List<String> ids) {
        Map<String, String> result = new HashMap<>();
        Set<String> remaining = new HashSet<>(ids);
        //L1 Cache查询本地缓存
        for (String id : ids) {
            String value = cache.getIfPresent(id);
            if (value != null) {
                result.put(id, value);
                remaining.remove(id);
            }
        }
        //L2 Cache查询redis
        for (String id : new HashSet<>(remaining)) {
            String key = deviceStatusPrefix + id;
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                result.put(id, value);
                cache.put(id, value);
                remaining.remove(id);
            }
        }

        if (remaining.isEmpty()) {
            return Result.ok(result);
        }
        //缓存若未命中，查询数据库
        int offset = RANDOM.nextInt(120 + 1);
        List<VirtualDevice> deviceStatus = this.lambdaQuery()
                .select(VirtualDevice::getDeviceCode, VirtualDevice::getStatus, VirtualDevice::getIsRunning)
                .in(VirtualDevice::getDeviceCode, remaining)
                .list();
        Map<String, String> map = deviceStatus.stream()
                .collect(Collectors.toMap(VirtualDevice::getDeviceCode,
                        device -> device.getStatus() + "," + device.getIsRunning()));
        result.putAll(map);
        map.forEach((key, value) -> {
            redisTemplate.opsForValue().set(deviceStatusPrefix + key, value, 180 + offset, TimeUnit.SECONDS);
            cache.put(key, value);
        });
        return Result.ok(result);
    }


    @Override
    public Result<String> changeTime(int time) {
        redisTemplate.opsForValue().set("Time", String.valueOf(time));
        return Result.ok(SuccessCode.TIME_CHANGE_SUCCESS.getCode(),
                SuccessCode.TIME_CHANGE_SUCCESS.getMessage());
    }

    @Override
    public Result<String> changeSeason(int season) {
        redisTemplate.opsForValue().set("Season", String.valueOf(season));
        return Result.ok(SuccessCode.SEASON_CHANGE_SUCCESS.getCode(), SuccessCode.SEASON_CHANGE_SUCCESS.getMessage());
    }

    /**
     * 将设备列表里面的水表的水闸设置为关
     */
    @Override
    public Result<String> closeValue(List<String> ids) {
        redisTemplate.opsForSet().add("meter_closed", ids.toArray(new String[0]));
        return Result.ok(SuccessCode.METER_CLOSE_SUCCESS.getCode(), SuccessCode.METER_CLOSE_SUCCESS.getMessage());
    }

    @Override
    public Result<String> open(List<String> ids) {
        redisTemplate.opsForSet().remove("meter_closed", ids.toArray());
        return Result.ok(SuccessCode.METER_OPEN_SUCCESS.getCode(), SuccessCode.METER_OPEN_SUCCESS.getMessage());
    }

    @Override
    public Result<String> openAllValue() {
        redisTemplate.delete("meter_closed");
        return Result.ok(SuccessCode.METER_OPEN_SUCCESS.getCode(), SuccessCode.METER_OPEN_SUCCESS.getMessage());
    }

    @Override
    public Result<String> closeAllValue() {
        Set<Object> keys = redisTemplate.opsForHash().keys("OnLineMap");
        List<String> list = keys.stream()
                .map(Object::toString)
                .filter(s -> !s.startsWith("2"))
                .toList();
        list.forEach(id -> redisTemplate.opsForSet().add("meter_closed", id));
        return Result.ok(SuccessCode.METER_CLOSE_SUCCESS.getCode(), SuccessCode.METER_CLOSE_SUCCESS.getMessage());
    }


    @Override
    public Result<String> destroyAll() {
        if (Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get("MeterChecked"))) == 1 ||
                Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get("WaterQualityChecked"))) == 1) {
            return Result.fail(ErrorCode.DEVICE_CANT_RESET_ERROR.code(), ErrorCode.DEVICE_CANT_RESET_ERROR.message());
        }
        this.isInit = false;
        cache.invalidateAll();
        redisTemplate.opsForValue().set("isInit", "0");
        return Result.ok(SuccessCode.DEVICE_RESET_SUCCESS.getCode(), SuccessCode.DEVICE_RESET_SUCCESS.getMessage());
    }


    /**
     * 将指定设备设置为下线状态
     *
     * @param ids 需要下线的设备ID列表
     * @return 下线结果
     */
    @Override
    public Result<String> offline(List<String> ids) {
        log.info("下线设备：{}", sanitizeForLog(ids.toString()));

        // 从运行设备集合中移除
        ids.forEach(runningDevices::remove);

        // 更新数据库：设置设备状态为离线，运行状态为false
        boolean updateResult = lambdaUpdate()
                .in(VirtualDevice::getDeviceCode, ids)
                .set(VirtualDevice::getStatus, "offline")
                .set(VirtualDevice::getIsRunning, false)
                .update();

        if (updateResult) {
            return Result.ok(SuccessCode.DEVICE_OFFLINE_SUCCESS.getCode(),
                    SuccessCode.DEVICE_OFFLINE_SUCCESS.getMessage());
        } else {
            return Result.fail(ErrorCode.SYSTEM_ERROR.code(), ErrorCode.SYSTEM_ERROR.message());
        }
    }

    /**
     * 更改设备模拟模式
     * <p>
     * 支持的模式：<p>
     * - burstPipe: 爆管模式<p>
     * - leaking: 漏水模式  <p>
     * - normal: 正常模式
     *
     * @param mode 模拟模式<p>
     * @return 更改结果
     */
    @Override
    public Result<String> changeMode(String mode) {
        // 验证模式参数的有效性
        if (mode.equals("burstPipe") || mode.equals("leaking") || mode.equals("normal") || mode.equals("shows")) {
            // 将模式存储到Redis中，供所有设备使用
            redisTemplate.opsForValue().set("mode", mode);
            return Result.ok(SuccessCode.METER_MODE_CHANGE_SUCCESS.getCode(),
                    SuccessCode.METER_MODE_CHANGE_SUCCESS.getMessage());
        }
        return Result.fail(ErrorCode.PARAM_VALIDATION_ERROR.code(), ErrorCode.PARAM_VALIDATION_ERROR.message());
    }

    /**
     * 获取设备总数
     *
     * @return 设备总数
     */
    @Override
    public Result<Integer> getDeviceNums() {
        return Result.ok(Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get("allDeviceNums"))));
    }

    /**
     * 对日志内容进行简单清洗，防止换行等导致日志注入
     *
     * @param input 原始输入字符串
     * @return 清洗后的安全字符串
     */
    private String sanitizeForLog(String input) {
        if (input == null) {
            return null;
        }
        // 去除回车和换行符，防止伪造多行日志
        return input.replace('\r', ' ')
                .replace('\n', ' ');
    }

    /**
     * 为指定设备启动心跳任务
     * <p>
     * 心跳任务会定期发送设备心跳信息，确保设备在线状态
     *
     * @param deviceId 设备ID
     */
    private void startHeartbeat(String deviceId) {
        // 获取心跳发送间隔（秒）和时间偏移（秒）
        int time = Integer.parseInt(serverConfig.getMeterReportFrequency()) / 1000;
        int offset = Integer.parseInt(serverConfig.getMeterTimeOffset()) / 1000;

        // 启动定时心跳任务
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            Long reportTime = this.reportTime.get(deviceId);

            // 检查设备是否还在运行，如果不在运行则停止心跳
            if (reportTime == null || !runningDevices.contains(deviceId)) {
                return;
            }

            // 如果距离上次上报超过10秒，使用当前时间作为心跳时间戳
            // 这样可以确保心跳时间戳的合理性
            reportTime = now - reportTime >= 10_000L ? now : reportTime;

            try {
                // 发送心跳信息
                dataSender.getObject().heartBeat(deviceId, reportTime);
            } catch (Exception e) {
                log.error("心跳发送异常: {}", e.getMessage(), e);
            }
        }, 0, time + offset, TimeUnit.SECONDS);  // 立即开始，每隔time+offset秒执行一次
    }

    /**
     * 核心递归调度逻辑，为指定设备启动数据上报任务
     * <p>
     * 该方法采用递归调度模式，每次上报完成后会自动调度下一次上报，
     * 形成持续的数据上报流。通过随机延迟避免设备集中上报。
     *
     * @param deviceId 设备ID
     */
    private void scheduleNextReport(String deviceId) {
        String sanitizedDeviceId = sanitizeForLog(deviceId);

        // 检查设备是否还在运行集合中，如果不在则停止调度
        if (!runningDevices.contains(deviceId)) {
            log.info("设备 {} 不在运行集合中, 停止上报调度", sanitizedDeviceId);
            return;
        }

        // 获取上报配置参数
        String reportFrequency = serverConfig.getMeterReportFrequency();  // 基础上报频率
        String timeOffset = serverConfig.getMeterTimeOffset();  // 时间偏移范围

        // 配置参数校验
        if (reportFrequency == null || timeOffset == null) {
            log.warn("设备 {} 上报配置缺失", sanitizedDeviceId);
            return;
        }

        // 计算随机上报延迟，打破设备集中上报的峰值
        // 延迟 = 基础频率 + 随机偏移，使上报时间更加分散
        long delay = Long.parseLong(reportFrequency) +
                ThreadLocalRandom.current().nextLong(Long.parseLong(timeOffset));

        // 调度单次数据上报任务
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                // 处理单个设备的数据上报
                processSingleDevice(deviceId);
            } catch (Exception e) {
                log.error("设备 {} 数据上报失败: {}", sanitizedDeviceId, e.getMessage(), e);
            } finally {
                // 无论成功失败，都要递归调度下一次上报
                // 更新上报时间戳，用于心跳对齐
                reportTime.put(deviceId, System.currentTimeMillis());
                scheduleNextReport(deviceId);
            }
        }, delay, TimeUnit.MILLISECONDS);

        // 保存任务句柄，用于后续取消操作
        deviceTasks.put(deviceId, future);
    }

    /**
     * 生成单条模拟数据并发送
     * <p>
     * 该方法为单个设备生成完整的水表数据，包括：<p>
     * - 水流量（根据时间和楼宇类型计算）<p>
     * - 水压（根据水流量计算）<p>
     * - 水温（根据时间和季节计算）<p>
     * - 时间戳和其他元数据
     *
     * @param id 设备编号
     */
    private void processSingleDevice(String id) {
        MeterDataBo dataBo = new MeterDataBo();

        // 获取模拟的时间参数（秒）
        int time = Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get("Time")));

        // 根据时间和楼宇类型生成水流量
        double flow = waterFlowGenerate(time, id);

        // 根据水流量计算对应的水压
        double pressure = waterPressureGenerate(flow, serverConfig);

        // 获取当前季节参数
        int season = Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get("Season")));

        // 根据季节设置水温计算的中间值和步长
        int mid, step;
        step = switch (season) {
            case 1 -> {  // 春季
                mid = 15;  // 中间温度15度
                yield 3;   // 步长3度
            }
            case 2 -> {  // 夏季
                mid = 22;  // 中间温度22度
                yield 5;   // 步长5度
            }
            case 3 -> {  // 秋季
                mid = 17;  // 中间温度17度
                yield 2;   // 步长2度
            }
            default -> { // 冬季
                mid = 6;   // 中间温度6度
                yield 2;   // 步长2度
            }
        };

        // 设置数据对象的基本信息
        dataBo.setDeviceId(id);  // 设备ID
        dataBo.setDevice(1);    // 设备类型标识

        // 设置时间戳，添加随机纳秒避免时间戳完全相同
        dataBo.setTimeStamp(LocalDateTime.now().plusNanos(ThreadLocalRandom.current().nextInt(1000000)));

        // 设置模拟数据
        dataBo.setFlow(flow);    // 水流量
        dataBo.setPressure(pressure);  // 水压
        dataBo.setWaterTem(waterTemperateGenerate(time, mid, step));  // 水温
        dataBo.setIsOpen(DeviceStatus.NORMAL);    // 设备开关状态
        dataBo.setStatus(DeviceStatus.NORMAL);     // 设备运行状态

        // 发送数据到消息队列或数据接收端
        dataSender.getObject().sendMeterData(dataBo);
    }

    /**
     * 根据时间和楼宇类型生成水流量
     * <p>
     * 该方法根据设备ID判断楼宇类型，并调用相应的流量生成算法：<p>
     * - 教学楼：使用教育区流量模式<p>
     * - 实验楼：使用实验区流量模式  <p>
     * - 宿舍楼：使用宿舍区流量模式
     *
     * @param time     模拟时间（从0点开始的秒数）
     * @param deviceId 设备ID
     * @return 水流量值（保留3位小数）
     */
    private double waterFlowGenerate(int time, String deviceId) {
        // 从设备ID中提取楼宇编号（第3-4位字符）
        int buildingNum = Integer.parseInt(deviceId.substring(2, 4));
        double flow;

        // 获取楼宇类型分界参数
        int education = Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get("device:educationBuildings")));   // 教学楼数量
        int experiment = Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get("device:experimentBuildings"))); // 实验楼数量

        // 根据楼宇编号选择对应的流量生成算法
        if (buildingNum <= education) {
            // 教学楼：使用教育区流量模式
            flow = getEducationFlow(time, deviceId);
        } else if (buildingNum <= experiment) {
            // 实验楼：使用实验区流量模式
            flow = getExperimentFlow(time, deviceId);
        } else {
            // 宿舍楼：使用宿舍区流量模式
            flow = getDormitoryFlow(time, deviceId);
        }

        // 保留3位小数后返回
        return keep3(flow);
    }

    /**
     * 每台设备可以上报活跃用水数据的剩余次数
     */
    Map<String, Integer> eachDeviceRemainingReportActiveWaterInfoCounts;
    /**
     * 每台设备可以上报活跃用水数据的次数的副本，用于后续时段
     */
    Map<String, Integer> temp;
    /**
     * 是否已经随机选择了上报活跃用水数据的水表设备
     */
    volatile boolean flag = false;
    /**
     * 同步锁对象，用于保护宿舍设备选择的并发操作
     */
    final Object lock = new Object();

    private double getDormitoryFlow(int time, String id) {
        double p = Math.random();
        int offset = ThreadLocalRandom.current().nextInt(10 * 60);
        double v = ThreadLocalRandom.current().nextDouble(1);
        //上报周期
        int fre = Integer.parseInt(serverConfig.getMeterReportFrequency()) / 1000;
        if (((time <= 7 * 3600) && (time >= 0)) || (time >= 23 * 3600) && (time <= 24 * 3600)) {
            if (p >= 0.8) {
                //模拟宿舍起夜上厕所行为，最多夜间两次
                int freCnt = 7 * 3600 / fre;
                double p1 = 1 - (2.0 / freCnt);
                if (v >= p1) {
                    return ThreadLocalRandom.current().nextDouble(0.1, 0.15);
                }
            }
        } else if (time > 7.25 * 3600 + offset && time <= 8 * 3600 + offset) {
            double wakeUpDormRate = serverConfig.getWakeUpDormRate();
            synchronized (lock) {
                if (!flag) {
                    eachDeviceRemainingReportActiveWaterInfoCounts = chooseDormitory(wakeUpDormRate);
                    if (eachDeviceRemainingReportActiveWaterInfoCounts != null) {
                        temp = new HashMap<>(eachDeviceRemainingReportActiveWaterInfoCounts);
                    }
                    flag = true;
                }
            }
            if (p >= 0.93) {
                if (eachDeviceRemainingReportActiveWaterInfoCounts != null) {
                    synchronized (lock) {
                        if (eachDeviceRemainingReportActiveWaterInfoCounts.containsKey(id)
                                && eachDeviceRemainingReportActiveWaterInfoCounts.get(id) > 0) {
                            Integer cnt = eachDeviceRemainingReportActiveWaterInfoCounts.get(id);
                            eachDeviceRemainingReportActiveWaterInfoCounts.put(id, --cnt);
                            return ThreadLocalRandom.current().nextDouble(0.2, 0.3);
                        }
                    }
                }
            }
        } else if (time > 8 * 3600 + offset && time <= 12.5 * 3600) {
            //剩余
            if (p >= 0.98) {
                synchronized (lock) {
                    if (temp.containsKey(id) && temp.get(id) > 0) {
                        Integer cnt = temp.get(id);
                        temp.put(id, --cnt);
                        return ThreadLocalRandom.current().nextDouble(0.12, 0.15);
                    } else return 0;
                }
            }
        } else if (time > 12.5 * 3600 && time <= 18 * 3600) {
            double freCnt = 7.5 * 3600 / fre;
            double p3 = 1 - (5.0 / freCnt);
            if (p >= p3) {
                return ThreadLocalRandom.current().nextDouble(0.07, 0.1);
            } else return 0;
        } else if (time >= 20 * 3600 && time <= 22 * 3600) {
            int freCnt = 3 * 3600 / fre;
            double p4 = 1 - (2.0 / freCnt);
            if (p >= p4) {
                return ThreadLocalRandom.current().nextDouble(0.12, 0.3);
            } else {
                return 0;
            }
        } else if (time >= 23.5 * 3600 && time <= 24 * 3600) {
            flag = false;
            return 0;
        } else {
            if (p > 0.99995) {
                return ThreadLocalRandom.current().nextDouble(0.02, 0.05);
            } else return 0;
        }
        return 0.0;
    }

    /**
     * 方法返回约总设备的0.v倍的宿舍设备数量
     *
     * @param v 倍数
     */
    private Map<String, Integer> chooseDormitory(double v) {
        //宿舍楼的编号都在实验楼之后，下标是左闭右开的，firstIndex是第一个宿舍楼的下标
        int firstIndex = Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue()
                .get("device:experimentBuildings")));
        Long size;
        size = redisTemplate.opsForSet().size("device:meter");
        if (size != null) {
            Map<Object, Object> entries = redisTemplate.opsForHash().randomEntries("OnLineMap", (long) (size * v));
            if (entries != null) {
                return entries.keySet().stream()
                        .map(Object::toString)
                        .filter(id -> id.startsWith("1"))
                        .filter(id -> Integer.parseInt(id.substring(2, 4)) > firstIndex)
                        .collect(Collectors.toMap(id -> id, id -> 15));
            }
        }
        return null;
    }

    /**
     * 是否已经获取了实验楼设备列表
     */
    volatile boolean isGetExperimentDevices = false;
    /**
     * 是否已经获取了教学楼设备列表
     */
    volatile boolean isGetEducationDevices = false;
    /**
     * 教学楼设备ID集合
     */
    volatile Set<String> educationIds;
    /**
     * 每个实验楼设备剩余上报活跃用水数据的次数
     */
    volatile Map<String, Integer> eachExperimentDeviceRemainingReportActiveWaterInfoCounts;
    /**
     * 实验楼设备同步锁对象
     */
    final Object lock1 = new Object();
    /**
     * 上午时段（8-12点）标志位
     */
    volatile boolean flag1 = false;
    /**
     * 下午时段（12-15点）标志位
     */
    volatile boolean flag2 = false;
    /**
     * 傍晚时段（15-18点）标志位
     */
    volatile boolean flag3 = false;
    /**
     * 晚间时段（18-22点）标志位
     */
    volatile boolean flag4 = false;

    private double getExperimentFlow(int time, String deviceId) {
        //正在运行试验的教室，用水量可能一直存在
        double p = ThreadLocalRandom.current().nextDouble(1);
        if (time >= 8 * 3600 && time <= 12 * 3600) {
            //这里两次检查的目的是提高并发能力。只有一个线程是需要加锁的，其他线程无需加锁，因此在外部只需检查标志位
            if (!flag1) {
                synchronized (lock1) {
                    if (!flag1) {
                        assignRunnableActiveDevice();
                        flag1 = true;
                    }
                }
            }
            if (p >= 0.95) {
                return returnFlow(deviceId, 0.01, 0.05);
            } else return 0;
        } else if (time > 12 * 3600 && time <= 15 * 3600) {
            if (!flag2) {
                synchronized (lock1) {
                    if (!flag2) {
                        assignRunnableActiveDevice();
                        flag2 = true;
                    }
                }
            }
            if (p >= 0.95) {
                return returnFlow(deviceId, 0.01, 0.05);
            } else return 0;
        } else if (time > 15 * 3600 && time <= 18 * 3600) {
            if (!flag3) {
                synchronized (lock1) {
                    if (!flag3) {
                        assignRunnableActiveDevice();
                        flag3 = true;
                    }
                }
            }
            if (p >= 0.95) {
                return returnFlow(deviceId, 0.1, 0.15);
            } else return 0;
        } else if (time > 18 * 3600 && time <= 22 * 3600) {
            if (!flag4) {
                synchronized (lock1) {
                    if (!flag4) {
                        assignRunnableActiveDevice();
                        flag4 = true;
                    }
                }
            }
            if (p >= 0.95) {
                return returnFlow(deviceId, 0.05, 0.1);
            } else return 0;
        } else if (time >= 23 * 3600 && time <= 24 * 3600) {
            //重置标志位，以便第二天选取不同的运行集合
            reset();
            return 0;
        } else {
            return 0;
        }
    }

    /**
     * 保证第二天可以正常分配上报活跃水信息的水表设备
     */
    private void reset() {
        flag1 = false;
        flag2 = false;
        flag3 = false;
        flag4 = false;
    }

    private double returnFlow(String deviceId, double origin, double bound) {
        Integer cnt = eachExperimentDeviceRemainingReportActiveWaterInfoCounts.get(deviceId);
        if (cnt != null && cnt > 0) {
            eachExperimentDeviceRemainingReportActiveWaterInfoCounts.put(deviceId, --cnt);
            return ThreadLocalRandom.current().nextDouble(origin, bound);
        } else return 0;
    }

    /**
     * 分配可以上报活跃水信息的设备
     */
    private void assignRunnableActiveDevice() {
        int education = Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get("device:educationBuildings")));
        int experiment = Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get("device:experimentBuildings")));
        synchronized (lock1) {
            if (!isGetExperimentDevices) {
                //设备全量列表
                Set<String> members = Objects.requireNonNull(redisTemplate.opsForSet().members("device:meter"));
                //是实验楼的所有设备数量
                int canRunning = (int) members.stream().filter(id -> {
                    int buildingNum = Integer.parseInt(id.substring(2, 4));
                    return buildingNum > education && buildingNum <= experiment;
                }).count();
                //选择的所有运行设备
                eachExperimentDeviceRemainingReportActiveWaterInfoCounts = members
                        .stream()
                        .filter(id -> {
                            int buildingNum = Integer.parseInt(id.substring(2, 4));
                            return buildingNum > education && buildingNum <= experiment;
                        })
                        .limit(Math.max(1, canRunning / 3))
                        .collect(Collectors.toMap(id -> id, id -> 60));
                isGetExperimentDevices = true;
            }
        }
    }

    private double getEducationFlow(int time, String deviceId) {
        // 教学区活跃时段（以小时为单位，0-23）
        // 假设：早上 8-12 点，下午 14-20 点
        boolean isActiveTime = (time >= 8 * 3600 && time <= 12 * 3600) || (time >= 14 * 3600 && time <= 21 * 3600);
        // 教学区设备集合，只第一次获取
        int education = Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get("device:educationBuildings")));
        synchronized (this) {
            if (!isGetEducationDevices) {
                Set<String> members = Objects.requireNonNull(redisTemplate.opsForSet().members("device:meter"));
                int canRunning = (int) members.stream()
                        .filter(id -> {
                            int building = Integer.parseInt(id.substring(2, 4));
                            return building <= education;
                        })
                        .count();
                educationIds = members.stream()
                        .filter(id -> {
                            int building = Integer.parseInt(id.substring(2, 4));
                            return building <= education;
                        })
                        .limit(Math.max(1, canRunning / 2))
                        .collect(Collectors.toSet());
                isGetEducationDevices = true;
            }
        }
        // 如果这个设备属于教学楼
        if (educationIds != null && educationIds.contains(deviceId)) {
            double p = ThreadLocalRandom.current().nextDouble(); // 0-1 随机概率
            if (!isActiveTime) {
                // 非活跃时间，绝大多数为 0
                if (p <= 0.99991) {
                    return 0.0;
                } else {
                    return ThreadLocalRandom.current().nextDouble(0.1, 0.15);
                }
            } else {
                // 活跃时间，偶尔有人使用水，流量低且离散
                if (p <= 0.9995) {
                    return 0.0; // 大多数时间没用水
                } else {
                    return ThreadLocalRandom.current().nextDouble(0.05, 0.15);
                }
            }
        } else {
            // 非教学楼设备，不处理
            return 0.0;
        }
    }

    /**
     * 根据每天不同时段生成水温的水温生成器
     *
     * @param time 时间
     * @param mid  季节基准值
     * @param step 昼夜温差
     */
    private double waterTemperateGenerate(double time, double mid, double step) {
        double pi = Math.PI;

        // 一天 = 86400 秒，14 点 = 14 * 3600 秒
        double phi = (pi / 2) - (2 * pi * 14 * 3600 / 86400);

        return keep3(
                mid + step * Math.sin(2 * pi * time / 86400 + phi)
        );
    }


    /**
     * 初始化设备并入库
     */
    @Time
    @Override
    public Result<String> init(int buildings, int floors, int rooms, int dormitoryBuildings,
                               int educationBuildings, int experimentBuildings) throws InterruptedException {
        if (isRunning()) {
            return Result.fail(ErrorCode.DEVICE_DEVICE_RUNNING_NOW_ERROR.code(),
                    ErrorCode.DEVICE_DEVICE_RUNNING_NOW_ERROR.message());
        }
        if (isInit) {
            return Result.fail(ErrorCode.DEVICE_ALREADY_INIT_ERROR.code()
                    , ErrorCode.DEVICE_ALREADY_INIT_ERROR.message());
        }
        String prefix = "device:";
        //清除上一次模拟数据
        clearRedisAndDbData(redisTemplate, deviceMapper);
        redisTemplate.opsForValue().set(prefix + "educationBuildings", String.valueOf(educationBuildings));
        redisTemplate.opsForValue().set(prefix + "experimentBuildings", String.valueOf(educationBuildings + experimentBuildings));

        DeviceIdList deviceIdList = initAllRedisData(buildings, floors, rooms, redisTemplate, redissonClient);

        List<String> meterDeviceIds = deviceIdList.getMeterDeviceIds();
        List<String> waterQualityDeviceIds = deviceIdList.getWaterQualityDeviceIds();

        List<VirtualDevice> meterList = new ArrayList<>();
        List<VirtualDevice> waterList = new ArrayList<>();

        //构建集合
        buildDevicesList(meterDeviceIds, meterList, 1);
        buildDevicesList(waterQualityDeviceIds, waterList, 2);
        VirtualMeterDeviceServiceImpl proxy = (VirtualMeterDeviceServiceImpl) AopContext.currentProxy();
        //异步写入数据库
        pool.submit(() -> proxy.saveBatch(meterList, 2000));
        pool.submit(() -> proxy.saveBatch(waterList, 2000));
        this.isInit = true;
        waterQualityDeviceService.setInit(true);
        log.info("设备注册完成：校区 3 楼宇 {} 层数 {} 房间 {}", buildings, floors, rooms);
        this.allSize = buildings * floors * rooms * 3;
        //总数量写入redis
        redisTemplate.opsForValue().set("allDeviceNums", String.valueOf(allSize));
        return Result.ok(SuccessCode.DEVICE_REGISTER_SUCCESS.getCode(),
                SuccessCode.DEVICE_REGISTER_SUCCESS.getMessage());
    }

    /**
     * 构建设备集合，方便批量存入数据库
     *
     * @param deviceIds         传入的集合总数
     * @param virtualDeviceList 目标集合
     */
    private static void buildDevicesList(List<String> deviceIds, List<VirtualDevice> virtualDeviceList, int type) {
        Date now = new Date();
        deviceIds.forEach(id -> {
            VirtualDevice virtualDevice = new VirtualDevice();
            virtualDevice.setDeviceType(type);
            virtualDevice.setDeviceCode(id);
            String sn = "JX" + UUID.fastUUID().toString().substring(0, 18).toUpperCase();
            virtualDevice.setInstallDate(now);
            virtualDevice.setSnCode(sn);
            virtualDevice.setCampusNo(id.substring(1, 2));
            virtualDevice.setBuildingNo(id.substring(2, 4));
            virtualDevice.setFloorNo(id.substring(4, 6));
            virtualDevice.setRoomNo(id.substring(6, 9));
            virtualDevice.setStatus("online");
            virtualDevice.setIsRunning(false);
            virtualDeviceList.add(virtualDevice);
        });
    }

    /**
     * 设备上线后置处理器
     */
    public void markDeviceOnline(String deviceCode, long timestamp, DeviceMapper deviceMapper,
                                 StringRedisTemplate redisTemplate) {
        //得到自定义线程池
        ExecutorService pools = getExecutorPools("markDeviceOnline", 5, 10, 60, 1000);
        // 1. 更新数据库状态（仅当当前为 offline 时）
        pools.submit(() -> {
            LambdaUpdateWrapper<VirtualDevice> updateWrapper =
                    new LambdaUpdateWrapper<VirtualDevice>()
                            .eq(VirtualDevice::getDeviceCode, deviceCode)
                            .eq(VirtualDevice::getStatus, "offline")
                            .set(VirtualDevice::getIsRunning, true)
                            .set(VirtualDevice::getStatus, "online");
            deviceMapper.update(updateWrapper);
        });

        // 2. 清理离线缓存
        redisTemplate.delete("device:OffLine:" + deviceCode);
        redisTemplate.delete("cache:device:status:" + deviceCode);
        cache.invalidate(deviceCode);
        // 3. 加入心跳监控
        redisTemplate.opsForHash()
                .put("OnLineMap", deviceCode, String.valueOf(timestamp));
    }

    public void madeSomeLocalCacheInvalidated(List<String> ids) {
        ids.forEach(cache::invalidate);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VirtualMeterDeviceServiceImpl that)) return false;
        return isInit == that.isInit && isRunning() == that.isRunning();
    }

    @Override
    public int hashCode() {
        return Objects.hash(isInit, isRunning());
    }

    private boolean isRunning() {
        return !runningDevices.isEmpty();
    }
}
