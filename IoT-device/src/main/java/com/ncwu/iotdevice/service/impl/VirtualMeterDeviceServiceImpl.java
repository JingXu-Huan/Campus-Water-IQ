package com.ncwu.iotdevice.service.impl;


import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncwu.common.VO.Result;
import com.ncwu.common.enums.ErrorCode;
import com.ncwu.common.enums.SuccessCode;
import com.ncwu.iotdevice.AOP.annotation.InitLuaScript;
import com.ncwu.iotdevice.AOP.annotation.Time;
import com.ncwu.iotdevice.Constants.DeviceStatus;
import com.ncwu.iotdevice.config.ServerConfig;
import com.ncwu.iotdevice.domain.Bo.DeviceIdList;
import com.ncwu.iotdevice.domain.Bo.MeterDataBo;
import com.ncwu.iotdevice.domain.entity.VirtualDevice;
import com.ncwu.iotdevice.enums.SwitchModes;
import com.ncwu.iotdevice.exception.MessageSendException;
import com.ncwu.iotdevice.mapper.DeviceMapper;
import com.ncwu.iotdevice.service.VirtualMeterDeviceService;
import com.ncwu.iotdevice.utils.Utils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ncwu.iotdevice.AOP.InitLuaScript.Lua_script;
import static com.ncwu.iotdevice.utils.Utils.keep3;
import static com.ncwu.iotdevice.utils.Utils.markDeviceOnline;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/21
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualMeterDeviceServiceImpl extends ServiceImpl<DeviceMapper, VirtualDevice> implements VirtualMeterDeviceService {
    Set<String> idList;
    int totalSize;
    AtomicLong sendCnt = new AtomicLong(0);
    final String meterStatusPrefix = "cache:meter:status:";
    private ScheduledExecutorService scheduler;

    @PostConstruct
    void init() {
        idList = new HashSet<>();
        //经实验验证，每100台设备对应一个线程即可。
        //取max防止入参为0发生异常
        scheduler = Executors.newScheduledThreadPool(Math.max(1, (int) (this.count() / 100)));
    }

    //开大小为5的线程池
    final ExecutorService pool = Executors.newFixedThreadPool(5);

    // 存储每个设备的调度句柄，用于精准停止模拟
    private final Map<String, ScheduledFuture<?>> deviceTasks = new ConcurrentHashMap<>();

    //设备上报数据的模式，默认就是正常模式
    public final SwitchModes currentMode = SwitchModes.NORMAL;

    //设备是否已经完成了初始化
    public volatile boolean isInit = false;

    //上报任务是否正在进行
    public volatile boolean isRunning = false;

    private final StringRedisTemplate redisTemplate;
    private final DeviceMapper deviceMapper;
    private final VirtualWaterQualityDeviceServiceImpl service;
    private final ServerConfig serverConfig;

    /**
     * 停止模拟时调用，优雅关闭资源
     */
    @PreDestroy
    public void destroy() {
        // 确保应用关闭时停止所有调度
        stopSimulation();
        // 确保应用关闭之后清空 redis 中所有数据
        Utils.clearRedisData(redisTemplate, deviceMapper);
        scheduler.shutdown();
    }

    /**
     * 前端入口：所有设备开始模拟
     */
    //获取方法耗时
    @Time
    //初始化 Lua 脚本
    @InitLuaScript
    public Result<String> start() {
        //检查设备初始化状态开关
        if (!isInit) {
            return Result.fail(ErrorCode.BUSINESS_INIT_ERROR.code(),
                    ErrorCode.PARAM_VALIDATION_ERROR.message());
        }
        //检查模拟器状态开关
        if (isRunning) {
            log.info("模拟器已在运行中");
            return Result.fail(ErrorCode.BUSINESS_DEVICE_RUNNING_NOW_ERROR.code(),
                    ErrorCode.PARAM_VALIDATION_ERROR.message());
        }
        //开始模拟
        this.isRunning = true;
        Set<String> ids = redisTemplate.opsForSet().members("device:meter");
        if (ids != null && !ids.isEmpty()) {
            pool.submit(() -> {
                this.lambdaUpdate().in(VirtualDevice::getDeviceCode, ids)
                        .eq(VirtualDevice::getStatus, "offline")
                        .set(VirtualDevice::getStatus, "online").update();
            });
            String hashKey = "OnLineMap";
            //使用Lua脚本加速redis操作！！！
            redisTemplate.execute(Lua_script, List.of(hashKey), "-1");
            // 每一个设备开启一个独立的递归流
            ids.forEach(this::scheduleNextReport);
            this.totalSize = ids.size();
            log.info("成功开启 {} 台设备的模拟数据流", ids.size());
            return Result.ok("成功开启" + ids.size() + "台设备");
        }
        return Result.fail(ErrorCode.UNKNOWN.code(), ErrorCode.UNKNOWN.message());
    }

    @Override
    public Result<String> startList(List<String> ids) {
        if (!isInit) {
            return Result.fail(ErrorCode.BUSINESS_INIT_ERROR.code(),
                    ErrorCode.BUSINESS_INIT_ERROR.message());
        }
        if (ids != null && !ids.isEmpty()) {
            pool.submit(() -> {
                this.lambdaUpdate().in(VirtualDevice::getDeviceCode, ids)
                        .eq(VirtualDevice::getStatus, "offline")
                        .set(VirtualDevice::getStatus, "online").update();
            });
            //删除缓存
            ids.forEach(id -> redisTemplate.delete(meterStatusPrefix + id));
            // 每一个设备开启一个独立的递归流
            ids.forEach(id -> redisTemplate.opsForHash().put("OnLineMap", id, "-1"));
            ids.forEach(this::scheduleNextReport);
            log.info("批量：成功开启 {} 台设备的模拟数据流", ids.size());
            return Result.ok("成功开启" + ids.size() + "台设备");
        }
        return Result.fail(ErrorCode.UNKNOWN.code(), ErrorCode.UNKNOWN.message());
    }

    /**
     * 前端入口：所有虚拟设备停止模拟
     */
    @Time
    @InitLuaScript
    public Result<String> stopSimulation() {
        //关闭开关
        this.isRunning = false;
        // 取消所有排队中的倒计时任务
        deviceTasks.forEach((id, future) -> {
            if (future != null && !future.isCancelled()) {
                future.cancel(false);
            }
        });
        //写回数据库状态，在异步线程池
        //todo 由于异步线程的异常不被事务控制，这里最好用消息队列
        pool.submit(() -> {
            LambdaUpdateWrapper<VirtualDevice> offline = new LambdaUpdateWrapper<VirtualDevice>()
                    .eq(VirtualDevice::getStatus, "online")
                    .set(VirtualDevice::getStatus, "offline");
            deviceMapper.update(offline);
        });
        //删缓存
        //此方法是安全的，使用scan进行删除
        Utils.redisScanDel(meterStatusPrefix + "*", 100, redisTemplate);
        String hashKey = "OnLineMap";
        redisTemplate.execute(Lua_script, List.of(hashKey), "0");
        deviceTasks.clear();
        log.info("已停止所有模拟数据上报任务");
        return Result.ok("已停止所有模拟数据上报任务");
    }

    /**
     * 前端入口：单设备或者批量设备停止模拟
     */
    public Result<String> singleStopSimulation(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.fail(null, "设备列表为空");
        }
        if (ids.size() >= 10000) {
            return Result.fail(null, "设备列表太多");
        }
        ids.forEach(id -> {
            ScheduledFuture<?> future = deviceTasks.get(id);
            if (future != null) {
                future.cancel(false);
                deviceTasks.remove(id);
                redisTemplate.opsForHash().put("OnLineMap", id, String.valueOf(0));
            }
        });
        //这里同理
        pool.submit(() -> {
            LambdaUpdateWrapper<VirtualDevice> offline = new LambdaUpdateWrapper<VirtualDevice>()
                    .in(VirtualDevice::getDeviceCode, ids)
                    .eq(VirtualDevice::getStatus, "online")
                    .set(VirtualDevice::getStatus, "offline");
            deviceMapper.update(offline);
            //删除缓存
            redisTemplate.delete(ids);

        });
        return Result.ok(SuccessCode.DEVICE_STOP_SUCCESS.getCode(),
                SuccessCode.DEVICE_STOP_SUCCESS.getMessage());
    }

    @Override
    public Result<Map<String, String>> checkDeviceStatus(List<String> ids) {
        HashMap<String, String> map = new HashMap<>();
        //这个 map 标记哪些元素存在于 redis
        Map<String, String> map1 = ids.stream()
                .collect(Collectors.toMap(Function.identity(), id -> "UNKNOWN"));
        Iterator<Map.Entry<String, String>> it = map1.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            String status = redisTemplate.opsForValue().get(meterStatusPrefix + entry.getKey());
            if (status != null) {
                map.put(entry.getKey(), status);
                it.remove();
            }
        }
        int offset = new Random().nextInt(120 + 1);
        map1.forEach((id, v) -> {
            String status = this.lambdaQuery().eq(VirtualDevice::getDeviceCode, id).one().getStatus();
            map.put(id, status);
            //加偏移量，防止缓存雪崩
            redisTemplate.opsForValue().set(meterStatusPrefix + id, status, 180 + offset, TimeUnit.SECONDS);
        });
        return Result.ok(map);
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

    @Override
    public Result<String> closeValue(List<String> ids) {
        return null;
    }

    @Override
    public Result<String> open(List<String> ids) {
        return null;
    }

    @Override
    public Result<String> openAllValue() {
        return null;
    }

    @Override
    public Result<String> closeAllValue() {
        return null;
    }

    /**
     * 核心递归调度逻辑
     */
    private void scheduleNextReport(String deviceId) {
        // 检查全局控制位
        if (!isRunning || currentMode != SwitchModes.NORMAL) {
            return;
        }

        String reportFrequency = serverConfig.getReportFrequency();
        String timeOffset = serverConfig.getTimeOffset();

        // 设定上报周期，例如平均 60s，上下浮动 2s (58000ms->62000ms)
        // 这样可以彻底打破所有设备在同一秒上报的情况
        if (reportFrequency == null || timeOffset == null) {
            return;
        }
        long delay = Long.parseLong(reportFrequency) + ThreadLocalRandom.current()
                .nextLong(Long.parseLong(timeOffset));
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                if (!isDeviceOnline(deviceId)) {
                    return;
                }
                processSingleDevice(deviceId);
            } catch (Exception e) {
                log.error("设备 {} 数据上报失败: {}", deviceId, e.getMessage());
            } finally {
                // 报完本次，如果没有被停掉，递归触发下一次上报
                if (isDeviceOnline(deviceId)) {
                    scheduleNextReport(deviceId);
                }
            }
        }, delay, TimeUnit.MILLISECONDS);

        deviceTasks.put(deviceId, future);
    }

    private boolean isDeviceOnline(String deviceId) {
        String s = (String) redisTemplate.opsForHash().get("OnLineMap", deviceId);
        long time = 0;
        if (s != null) {
            time = Long.parseLong(s);
        }
        return time != 0;
    }

    /**
     * 生成单条模拟数据并执行发送
     *
     * @param id 设备编号
     */
    private void processSingleDevice(String id) {
        //多线程环境下应当使用原子
        sendCnt.incrementAndGet();
        if (idList.size() < totalSize * 0.05) {
            idList.add(id);
        }
        MeterDataBo dataBo = new MeterDataBo();
        dataBo.setDeviceId(id);
        dataBo.setDevice(1);
        int time = Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get("Time")));
        double flow;
        if (time <= 5 && idList.contains(id)) {
            //夜间漏水
            flow = keep3(0.1 + ThreadLocalRandom.current().nextDouble(0.05));
        }
        else {
            flow = waterFlowGenerate(time);
        }
        double pressure = waterPressureGenerate(flow);
        // 时间戳微扰：增加纳秒级偏移，使排序更逼真
        dataBo.setTimeStamp(LocalDateTime.now().plusNanos(ThreadLocalRandom.current().nextInt(1000000)));
        dataBo.setFlow(flow);
        // 计算增量并累加到 Redis。假设采集频率约 10s，增量 = 流量 * 时间
        double increment = keep3(flow * 10);
        Double currentTotal = redisTemplate.opsForHash().increment("meter:total_usage", id, increment);
        dataBo.setTotalUsage(keep3(currentTotal));
        dataBo.setPressure(pressure);
        String season = Objects.requireNonNull(redisTemplate.opsForValue().get("Season"));
        int s = Integer.parseInt(season);
        int mid, step;
        //春季
        if (s == 1) {
            mid = 15;
            step = 3;
        }
        //夏季
        else if (s == 2) {
            mid = 22;
            step = 5;
        }
        //秋季
        else if (s == 3) {
            mid = 17;
            step = 2;
        }
        //冬季
        else {
            mid = 6;
            step = 2;
        }
        dataBo.setWaterTem(waterTemperateGenerate(time * 3600L + sendCnt.get() * 10, mid, step));
        dataBo.setIsOpen(DeviceStatus.NORMAL);
        dataBo.setStatus(DeviceStatus.NORMAL);
        sendData(dataBo);
    }

    /**
     * 根据管网水流对水压进行计算和离散化
     */
    private double waterPressureGenerate(double flow) {
        //管网初始压力
        double p0 = serverConfig.getP0();
        double pressure = p0 - 0.15 * flow + ThreadLocalRandom.current().nextDouble(0.01, 0.03);
        //离散步长
        double s = serverConfig.getStep();
        double Pdiscrete = Math.round(pressure / s) * s;
        //管网最小压力
        double Pmin = serverConfig.getPmin();
        //管网最大压力
        double Pmax = serverConfig.getPmax();
        return keep3(Math.min(Math.max(Pdiscrete, Pmin), Pmax));
    }

    /**
     * 根据每天不同时段生成水流量的水流生成器
     */
    private double waterFlowGenerate(int time) {
        double flow;
        if (time >= 0 && time <= 5) {
            double p = Math.random();
            if (p <= 0.8) {
                flow = 0.0;
            } else if (p <= 0.95) {
                flow = ThreadLocalRandom.current().nextDouble(0, 0.15);
            } else {
                flow = ThreadLocalRandom.current().nextDouble(0.1, 0.15);
            }
        } else if (time <= 8) {
            if (Math.random() > 0.8) {
                flow = 0;
            } else flow = ThreadLocalRandom.current().nextDouble(0.15, 0.25);
        } else if (time <= 17) {
            if (Math.random() > 0.8) {
                flow = 0;
            } else flow = ThreadLocalRandom.current().nextDouble(0.08, 0.15);
        } else if (time <= 22) {
            if (Math.random() > 0.8) {
                flow = 0;
            } else flow = ThreadLocalRandom.current().nextDouble(0.2, 0.35);
        } else {
            double p = Math.random();
            if (p <= 0.6) {
                flow = 0;
            } else if (p <= 0.8) {
                flow = ThreadLocalRandom.current().nextDouble(0, 0.05);
            } else {
                flow = ThreadLocalRandom.current().nextDouble(0, 0.15);
            }
        }
        return keep3(flow);
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
     * 初始化设备并入库（逻辑保持原样）
     */
    @Time
    public Result<String> init(int buildings, int floors, int rooms) throws InterruptedException {

        Utils.clearRedisData(redisTemplate, deviceMapper);
        DeviceIdList deviceIdList = Utils.initAllRedisData(buildings, floors, rooms, redisTemplate);

        List<String> meterDeviceIds = deviceIdList.getMeterDeviceIds();
        List<String> waterQualityDeviceIds = deviceIdList.getWaterQualityDeviceIds();

        List<VirtualDevice> meterList = new ArrayList<>();
        List<VirtualDevice> waterList = new ArrayList<>();

        build(meterDeviceIds, meterList, 1);
        build(waterQualityDeviceIds, waterList, 2);

        ExecutorService executor = Executors.newFixedThreadPool(4);
        VirtualMeterDeviceServiceImpl proxy = (VirtualMeterDeviceServiceImpl) AopContext.currentProxy();
        executor.submit(() -> proxy.saveBatch(meterList, 2000));
        executor.submit(() -> proxy.saveBatch(waterList, 2000));
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);
        this.isInit = true;
        service.setInit(true);
        log.info("设备注册完成：楼宇 {} 层数 {} 房间 {}", buildings, floors, rooms);
        return Result.ok(SuccessCode.DEVICE_REGISTER_SUCCESS.getCode(),
                SuccessCode.DEVICE_REGISTER_SUCCESS.getMessage());
    }

    @Override
    public String isRunning() {
        return String.valueOf(this.isRunning);
    }

    @Override
    public String isInit() {
        return String.valueOf(this.isInit);
    }

    //season
    @Override
    public String getCurrentMode() {
        return String.valueOf(this.currentMode);
    }

    /**
     * 此方法传递发送的数据，并且更新 redis 中设备的在在线状态
     *
     * @param dataBo 数据载荷
     * @throws MessageSendException 数据发送失败异常
     */
    private void sendData(MeterDataBo dataBo) throws MessageSendException {
        // 模拟发送，实际可接入消息队列
        //log.debug("上报数据: {}", dataBo);
        //更新 redis 中时间戳,就是向 redis 上报自己的心跳
        //获取当前系统时间
        long timestamp = System.currentTimeMillis();
        String id = dataBo.getDeviceId();
        redisTemplate.opsForHash().put("OnLineMap", id, String.valueOf(timestamp));
        //todo 消息队列通知上线

        Boolean onLine = redisTemplate.hasKey("device:OffLine:" + id);
        if (onLine) {
            //如果设备上线,调用设备上线后置处理器
            markDeviceOnline(id, timestamp, deviceMapper, redisTemplate);
        }
        System.out.println(dataBo);
    }


    private static void build(List<String> deviceIds, List<VirtualDevice> virtualDeviceList, int type) {
        Date now = new Date();
        deviceIds.forEach(id -> {
            VirtualDevice virtualDevice = new VirtualDevice();
            virtualDevice.setDeviceType(type);
            virtualDevice.setDeviceCode(id);
            String sn = "JX" + UUID.fastUUID().toString().substring(0, 18).toUpperCase();
            virtualDevice.setInstallDate(now);
            virtualDevice.setSnCode(sn);
            virtualDevice.setBuildingNo(id.substring(1, 3));
            virtualDevice.setFloorNo(id.substring(3, 5));
            virtualDevice.setRoomNo(id.substring(5, 8));
            virtualDevice.setStatus("online");
            virtualDeviceList.add(virtualDevice);
        });
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VirtualMeterDeviceServiceImpl that)) return false;
        return isInit == that.isInit && isRunning == that.isRunning && currentMode == that.currentMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentMode, isInit, isRunning);
    }
}
