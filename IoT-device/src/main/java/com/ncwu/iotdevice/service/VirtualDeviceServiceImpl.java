package com.ncwu.iotdevice.service;


import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncwu.common.VO.Result;
import com.ncwu.iotdevice.Constants.DeviceStatus;
import com.ncwu.iotdevice.domain.Bo.DeviceIdList;
import com.ncwu.iotdevice.domain.Bo.MeterDataBo;
import com.ncwu.iotdevice.domain.entity.VirtualDevice;
import com.ncwu.iotdevice.enums.SwitchModes;
import com.ncwu.iotdevice.exception.DeviceRegisterException;
import com.ncwu.iotdevice.exception.MessageSendException;
import com.ncwu.iotdevice.mapper.DeviceMapper;
import com.ncwu.iotdevice.utils.Utils;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/21
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualDeviceServiceImpl extends ServiceImpl<DeviceMapper, VirtualDevice> implements VirtualDeviceService {

    // 线程池大小建议根据设备规模调整，对于 1000 个以内的设备，20-50 个线程足够，因为大多在等待
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(20);

    //开大小为5的线程池
    ExecutorService pool = Executors.newFixedThreadPool(5);

    // 存储每个设备的调度句柄，用于精准停止模拟
    private final Map<String, ScheduledFuture<?>> deviceTasks = new ConcurrentHashMap<>();

    //设备上报数据的模式，默认就是正常模式
    public SwitchModes currentMode = SwitchModes.NORMAL;

    //设备是否已经完成了初始化
    public volatile boolean isInit = false;

    //上报任务是否正在进行
    public volatile boolean isRunning = false;

    private final StringRedisTemplate redisTemplate;
    private final DeviceMapper deviceMapper;
//    private final VirtualDeviceService virtualDeviceService;

    /**
     * 停止模拟时调用，优雅关闭资源
     */
    @PreDestroy
    public void destroy() {
        // 确保应用关闭时停止所有调度
        stopSimulation();
        // 确保应用关闭之后清空 redis 中所有数据
        Utils.clearRedisData(redisTemplate);
        scheduler.shutdown();
    }

    /**
     * 前端入口：所有设备开始模拟
     */
    public Result<String> start() {
        //检查设备初始化状态开关
        if (!isInit) {
            return Result.fail(200, "请先执行 init 进行设备初始化");
        }
        //检查模拟器状态开关
        if (isRunning) {
            log.info("模拟器已在运行中");
            return Result.fail(200, "模拟器已在运行中");
        }
        //开始模拟
        this.isRunning = true;
        Set<String> ids = redisTemplate.opsForSet().members("device:meter");
        if (ids != null && !ids.isEmpty()) {
            // 每一个设备开启一个独立的递归流
            ids.forEach(this::scheduleNextReport);
            log.info("成功开启 {} 台设备的模拟数据流", ids.size());
            return Result.ok("成功开启" + ids.size() + "台设备");
        }
        return Result.fail(null, 500, "未知错误");
    }

    @Override
    public Result<String> startList(List<String> ids) {
        if (!isInit) {
            return Result.fail(200, "请先执行 init 进行设备初始化");
        }
        if (isRunning) {
            log.info("模拟器已在运行中");
            return Result.fail(200, "模拟器已在运行中");
        }
        this.isRunning = true;
        if (ids != null && !ids.isEmpty()) {
            // 每一个设备开启一个独立的递归流
            ids.forEach(this::scheduleNextReport);
            log.info("成功开启 {} 台设备的模拟数据流", ids.size());
            return Result.ok("成功开启" + ids.size() + "台设备");
        }
        return Result.fail(null, 500, "未知错误");
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
        deviceTasks.clear();
        log.info("已停止所有模拟数据上报任务");
        return Result.ok("已停止所有模拟数据上报任务");
    }

    /**
     * 前端入口：单设备或者批量设备停止模拟
     */
    public void singleStopSimulation(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        if (ids.size() >= 1000) {
            return;
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
        });
    }

    /**
     * 核心递归调度逻辑
     */
    private void scheduleNextReport(String deviceId) {
        // 检查全局控制位
        if (!isRunning || currentMode != SwitchModes.NORMAL) {
            return;
        }
        // 设定上报周期，例如平均 10s，上下浮动 2s (8000ms - 12000ms)
        // 这样可以彻底打破所有设备在同一秒上报的情况
        long delay = 8000 + new Random().nextInt(4000);
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
        double flow = random.nextDouble(0.05, 0.3);
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
        Utils.clearRedisData(redisTemplate);
        DeviceIdList deviceIdList = Utils.initAllRedisData(buildings, floors, rooms, redisTemplate);

        List<String> meterDeviceIds = deviceIdList.getMeterDeviceIds();
        List<String> waterQualityDeviceIds = deviceIdList.getWaterQualityDeviceIds();

        List<VirtualDevice> meterList = new ArrayList<>();
        List<VirtualDevice> waterList = new ArrayList<>();

        build(meterDeviceIds, meterList, 1);
        build(waterQualityDeviceIds, waterList, 2);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> this.saveBatch(meterList, 2000));
        executor.submit(() -> this.saveBatch(waterList, 2000));
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);
        isInit = true;
        log.info("设备注册完成：楼宇 {} 层数 {} 房间 {}", buildings, floors, rooms);
        return Result.ok(null, 200, "设备注册完成");
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

    private void sendData(MeterDataBo dataBo) throws MessageSendException {
        // 模拟发送，实际可接入消息队列
//        log.debug("上报数据: {}", dataBo);
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
        if (!(o instanceof VirtualDeviceServiceImpl that)) return false;
        return isInit == that.isInit && isRunning == that.isRunning && currentMode == that.currentMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentMode, isInit, isRunning);
    }
}
