package com.ncwu.iotdevice.service;


import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncwu.common.VO.Result;
import com.ncwu.common.enums.ErrorCode;
import com.ncwu.common.enums.SuccessCode;
import com.ncwu.iotdevice.Constants.DeviceStatus;
import com.ncwu.iotdevice.domain.Bo.DeviceIdList;
import com.ncwu.iotdevice.domain.Bo.MeterDataBo;
import com.ncwu.iotdevice.domain.entity.VirtualDevice;
import com.ncwu.iotdevice.enums.SwitchModes;
import com.ncwu.iotdevice.exception.MessageSendException;
import com.ncwu.iotdevice.mapper.DeviceMapper;
import com.ncwu.iotdevice.utils.Utils;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/21
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualDeviceServiceImpl extends ServiceImpl<DeviceMapper, VirtualDevice> implements VirtualDeviceService {

    final String meterStatusPrefix = "cache:meter:status:";
    final String onLineStatusPrefix = "Online:";

    // 线程池大小建议根据设备规模调整，对于 1000 个以内的设备，20-50 个线程足够，因为大多在等待
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(20);

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
            // 每一个设备开启一个独立的递归流
            ids.forEach(this::scheduleNextReport);
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
            ids.forEach(this::scheduleNextReport);
            log.info("成功开启 {} 台设备的模拟数据流", ids.size());
            return Result.ok("成功开启" + ids.size() + "台设备");
        }
        return Result.fail(ErrorCode.UNKNOWN.code(), ErrorCode.UNKNOWN.message());
    }

    /**
     * 前端入口：所有虚拟设备停止模拟
     */
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
        return Result.ok(SuccessCode.DEVICE_OPEN_SUCCESS.getCode(),
                SuccessCode.DEVICE_OPEN_SUCCESS.getMessage());
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

    /**
     * 核心递归调度逻辑
     */
    private void scheduleNextReport(String deviceId) {
        // 检查全局控制位
        if (!isRunning || currentMode != SwitchModes.NORMAL) {
            return;
        }
        // 设定上报周期，例如平均 60s，上下浮动 2s (58000ms->62000ms)
        // 这样可以彻底打破所有设备在同一秒上报的情况
        long delay = 58000L + ThreadLocalRandom.current().nextLong(2001);
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                processSingleDevice(deviceId);
            } catch (Exception e) {
                log.error("设备 {} 数据上报失败: {}", deviceId, e.getMessage());
            } finally {
                // 报完本次，递归触发下一次上报
                scheduleNextReport(deviceId);
            }
        }, delay, TimeUnit.MILLISECONDS);

        deviceTasks.put(deviceId, future);
    }

    /**
     * 生成单条模拟数据并执行发送
     */
    private void processSingleDevice(String id) {
        Random random = new Random();
        MeterDataBo dataBo = new MeterDataBo();
        dataBo.setDevice(1);
        dataBo.setDeviceId(id);
        // 时间戳微扰：增加纳秒级偏移，使排序更逼真
        dataBo.setTimeStamp(LocalDateTime.now().plusNanos(random.nextInt(1000000)));
        // 模拟瞬时流量 L/s
        double flow = random.nextDouble(0.0, 0.4);
        dataBo.setFlow(flow);
        // 计算增量并累加到 Redis。假设采集频率约 10s，增量 = 流量 * 时间
        double increment = flow * 10;
        Double currentTotal = redisTemplate.opsForHash().increment("meter:total_usage", id, increment);
        dataBo.setTotalUsage(currentTotal);
        dataBo.setPressure(random.nextDouble(0.2, 0.35));
        dataBo.setWaterTem(random.nextDouble(20, 37.5));
        dataBo.setIsOpen(DeviceStatus.NORMAL);
        dataBo.setStatus(DeviceStatus.NORMAL);

        sendData(dataBo);
    }

    /**
     * 初始化设备并入库（逻辑保持原样）
     */
    public Result<String> init(int buildings, int floors, int rooms) throws InterruptedException {

        Utils.clearRedisData(redisTemplate, deviceMapper);
        DeviceIdList deviceIdList = Utils.initAllRedisData(buildings, floors, rooms, redisTemplate);

        List<String> meterDeviceIds = deviceIdList.getMeterDeviceIds();
        List<String> waterQualityDeviceIds = deviceIdList.getWaterQualityDeviceIds();

        List<VirtualDevice> meterList = new ArrayList<>();
        List<VirtualDevice> waterList = new ArrayList<>();

        build(meterDeviceIds, meterList, 1);
        build(waterQualityDeviceIds, waterList, 2);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        VirtualDeviceServiceImpl proxy = (VirtualDeviceServiceImpl) AopContext.currentProxy();
        executor.submit(() -> proxy.saveBatch(meterList, 2000));
        executor.submit(() -> proxy.saveBatch(waterList, 2000));
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);
        isInit = true;
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
            afterOnLineProcessor(id, timestamp);
        }
        System.out.println(dataBo);
    }

    /**
     * 设备上线后置处理器
     *
     * @param id        设备编号
     * @param timestamp 当前系统时间戳
     */
    private void afterOnLineProcessor(String id, long timestamp) {
        LambdaUpdateWrapper<VirtualDevice> updateWrapper = new LambdaUpdateWrapper<VirtualDevice>()
                .eq(VirtualDevice::getDeviceCode, id)
                .eq(VirtualDevice::getStatus, "offline")
                .set(VirtualDevice::getStatus, "online");
        deviceMapper.update(updateWrapper);
        //后置处理：在离线缓存中将其移除
        redisTemplate.delete("device:OffLine:" + id);
        //后置处理：在心跳表中重新监控
        redisTemplate.opsForHash().put("OnLineMap", id, String.valueOf(timestamp));
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
        if (!(o instanceof VirtualDeviceServiceImpl that)) return false;
        return isInit == that.isInit && isRunning == that.isRunning && currentMode == that.currentMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentMode, isInit, isRunning);
    }
}
