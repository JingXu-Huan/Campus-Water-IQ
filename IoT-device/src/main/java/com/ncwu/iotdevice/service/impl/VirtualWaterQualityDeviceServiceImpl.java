package com.ncwu.iotdevice.service.impl;

import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncwu.common.VO.Result;
import com.ncwu.common.enums.ErrorCode;
import com.ncwu.common.enums.SuccessCode;
import com.ncwu.iotdevice.config.ServerConfig;
import com.ncwu.iotdevice.domain.Bo.WaterQualityDataBo;
import com.ncwu.iotdevice.domain.entity.VirtualDevice;
import com.ncwu.iotdevice.exception.DeviceRegisterException;
import com.ncwu.iotdevice.mapper.DeviceMapper;
import com.ncwu.iotdevice.service.DataSender;
import com.ncwu.iotdevice.service.VirtualWaterQualityDeviceService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static com.ncwu.iotdevice.utils.Utils.keep3;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/29
 */
@Getter
@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualWaterQualityDeviceServiceImpl extends ServiceImpl<DeviceMapper, VirtualDevice>
        implements VirtualWaterQualityDeviceService {

    private final Set<String> runningDevices = ConcurrentHashMap.newKeySet();

    final String deviceStatusPrefix = "cache:device:status:";

    //存储设备上报时间戳，以便和心跳对齐
    private final ConcurrentHashMap<String, Long> reportTime = new ConcurrentHashMap<>();

    //开大小为5的线程池
    final ExecutorService pool = Executors.newFixedThreadPool(10);

    //设备是否已经完成了初始化
    @Setter
    public volatile boolean isInit = false;
    private ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> deviceTasks = new ConcurrentHashMap<>();

    private final DeviceMapper deviceMapper;
    private final StringRedisTemplate redisTemplate;
    private final ServerConfig serverConfig;
    private final DataSender dataSender;

    @PostConstruct
    void init() {
        Long size = redisTemplate.opsForSet().size("device:sensor");
        if (size == null) {
            throw new DeviceRegisterException("请先初始化设备");
        }
        scheduler = new ScheduledThreadPoolExecutor(Math.max(2, (int) (size / 100L)));
    }

    @Override
    public Result<String> startAll() {
        if (!isInit) {
            return Result.fail(ErrorCode.DEVICE_ERROR.code(), ErrorCode.DEVICE_INIT_ERROR.message());
        }
        Long size1 = redisTemplate.opsForSet().size("device:sensor");
        if (size1 == null) {
            return Result.fail(ErrorCode.UNKNOWN.code(), ErrorCode.UNKNOWN.message());
        }
        if (runningDevices.size() == size1) {
            //如果所有设备已经存在于运行设备集合中，无需继续
            return Result.fail(ErrorCode.DEVICE_DEVICE_RUNNING_NOW_ERROR.code(),
                    ErrorCode.DEVICE_DEVICE_RUNNING_NOW_ERROR.message());
        }
        Set<String> ids = redisTemplate.opsForSet().members("device:sensor");
        if (ids == null) {
            return Result.fail(ErrorCode.UNKNOWN.code(), ErrorCode.UNKNOWN.message());
        }
        List<String> list = ids.stream().toList();
        runningDevices.addAll(list);
        if (ids.isEmpty()) {
            return Result.fail(ErrorCode.DEVICE_ERROR.code(), ErrorCode.DEVICE_INIT_ERROR.message());
        }
        pool.submit(() -> {
            //修改数据库状态
            LambdaUpdateChainWrapper<VirtualDevice> wrapper = this.lambdaUpdate()
                    .in(VirtualDevice::getId, ids)
                    .eq(VirtualDevice::getStatus, "offline")
                    .set(VirtualDevice::getStatus, "online")
                    .set(VirtualDevice::getIsRunning, true);
            this.update(wrapper);
        });
        //递归上报数据
        ids.forEach(this::scheduleNextReport);
        //递归上报心跳
        ids.forEach(this::startHeartbeat);
        log.info("成功开启{}台设备的数据流", ids.size());
        runningDevices.addAll(ids);
        //可以受检
        redisTemplate.opsForValue().set("WaterQualityChecked", "1");
        return Result.ok(SuccessCode.DEVICE_OPEN_SUCCESS.getCode(),
                SuccessCode.DEVICE_OPEN_SUCCESS.getMessage());
    }

    @Override
    public Result<String> startList(List<String> ids) {
        if (!isInit) {
            return Result.fail(ErrorCode.DEVICE_ERROR.code(), ErrorCode.DEVICE_INIT_ERROR.message());
        }
        runningDevices.addAll(ids);
        ids.forEach(this::scheduleNextReport);
        ids.forEach(this::startHeartbeat);
        //更新数据库状态,异步执行
        pool.submit(() -> {
            this.lambdaUpdate().in(VirtualDevice::getDeviceCode, ids)
                    .set(VirtualDevice::getStatus, "online")
                    .set(VirtualDevice::getIsRunning, true);
        });
        //删除缓存
        redisTemplate.delete(deviceStatusPrefix);
        log.info("批量：成功开启{}台设备的数据流", ids.size());
        return Result.ok(SuccessCode.DEVICE_OPEN_SUCCESS.getCode(),
                SuccessCode.DEVICE_OPEN_SUCCESS.getMessage());
    }

    @Override
    public Result<String> stopAll() {
        //停止受检
        redisTemplate.opsForValue().set("WaterQualityChecked", "0");
        runningDevices.clear();
        //立即停止未进行的调度任务，正在运行的调度任务将再下一次上报时停止。
        deviceTasks.forEach((id, future) -> {
            if (future != null && !future.isCancelled()) {
                future.cancel(false);
            }
        });
        return Result.ok(SuccessCode.DEVICE_STOP_SUCCESS.getCode(), SuccessCode.DEVICE_STOP_SUCCESS.getMessage());
    }

    @Override
    public Result<String> offLine(List<String> ids) {
        log.info("下线设备：{}", sanitizeForLog(ids.toString()));
        pool.submit(() -> {
            lambdaUpdate()
                    .in(VirtualDevice::getDeviceCode, ids)
                    .set(VirtualDevice::getStatus, "offline")
                    .set(VirtualDevice::getIsRunning, false)
                    .update();
        });
        ids.forEach(runningDevices::remove);
        //写入离线缓存列表
//        redisTemplate.executePipelined((RedisCallback<Object>) connection->{
//            for(String id:ids){
//                String key = "device:OffLine:" + id;
//                String value = "offlineBySystem";
//                redisTemplate.opsForValue().set(key,value);
//            }
//            return null;
//        });
        return Result.ok(SuccessCode.DEVICE_OFFLINE_SUCCESS.getCode(), SuccessCode.DEVICE_OFFLINE_SUCCESS.getMessage());
    }

    /**
     * 对日志内容进行简单清洗，防止换行等导致日志注入
     */
    private String sanitizeForLog(String input) {
        if (input == null) {
            return null;
        }
        // 去除回车和换行，防止伪造多行日志
        return input.replace('\r', ' ')
                .replace('\n', ' ');
    }

    @Override
    public Result<String> stopList(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.fail(ErrorCode.UNKNOWN.code(), ErrorCode.UNKNOWN.message());
        }
        //移除运行中列表
        ids.forEach(runningDevices::remove);
        //停止未运行的任务
        ids.forEach(id -> {
            ScheduledFuture<?> future = deviceTasks.get(id);
            if (future != null) {
                future.cancel(false);
                deviceTasks.remove(id);
                runningDevices.remove(id);
            }
        });
        pool.submit(() -> {
            this.lambdaUpdate().in(VirtualDevice::getDeviceCode, ids)
                    .set(VirtualDevice::getIsRunning, false)
                    .update();
        });
        return Result.ok(SuccessCode.DEVICE_STOP_SUCCESS.getCode(), SuccessCode.DEVICE_STOP_SUCCESS.getMessage());
    }

    @Override
    public Result<String> destroyAll() {
        if (isRunning()) {
            return Result.fail(ErrorCode.DEVICE_DEVICE_RUNNING_NOW_ERROR.code(),
                    ErrorCode.DEVICE_DEVICE_RUNNING_NOW_ERROR.message());
        } else {
            this.isInit = false;
            return Result.ok(SuccessCode.DEVICE_REGISTER_SUCCESS.getCode(),
                    SuccessCode.DEVICE_REGISTER_SUCCESS.getMessage());
        }
    }

    private void scheduleNextReport(String deviceId) {
        if (!runningDevices.contains(deviceId)) {
            return;
        }
        //读取上报时间
        String reportFrequency = serverConfig.getMeterReportFrequency();
        String timeOffset = serverConfig.getMeterTimeOffset();
        if (reportFrequency == null) {
            return;
        }
        long delay = Long.parseLong(reportFrequency) + ThreadLocalRandom.current()
                .nextLong(Long.parseLong(timeOffset));
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                reportTime.put(deviceId, System.currentTimeMillis());
                processSingleDevice(deviceId);
                scheduleNextReport(deviceId);
            } catch (Exception e) {
                log.error("设备:{}上报失败,原因:{}", sanitizeForLog(deviceId), e.getMessage());
            }
        }, delay, TimeUnit.MILLISECONDS);
        deviceTasks.put(deviceId, future);
    }

    /**
     * 生成水质模拟器的数据
     *
     * @param deviceId 设备编号
     */
    private void processSingleDevice(String deviceId) {
        WaterQualityDataBo dataBo = new WaterQualityDataBo();
        dataBo.setTimeStamp(LocalDateTime.now());
        dataBo.setDevice(2);
        dataBo.setDeviceId(deviceId);
        dataBo.setPh(keep3(ThreadLocalRandom.current().nextDouble(6.5, 8.5)));
        dataBo.setTurbidity(keep3(ThreadLocalRandom.current().nextDouble(0.8, 1.0)));
        dataBo.setChlorine(keep3(ThreadLocalRandom.current().nextDouble(0.05, 0.2)));
        dataBo.setStatus("normal");
        sendData(dataBo);
    }

    private void sendData(WaterQualityDataBo dataBo) {
        dataSender.sendWaterQualityData(dataBo);
    }

    private boolean isRunning() {
        return !runningDevices.isEmpty();
    }

    // 单独开一个心跳任务
    private void startHeartbeat(String deviceId) {
        int time = Integer.parseInt(serverConfig.getWaterQualityReportFrequency()) / 1000;
        int offset = Integer.parseInt(serverConfig.getWaterQualityReportTimeOffset()) / 1000;
        scheduler.scheduleAtFixedRate(() -> {
            Long reportTime = this.reportTime.get(deviceId);
            if (reportTime == null) {
                return;
            }
            try {
                //只有不在线的设备，我们才停止心跳的上报
                if (!isDeviceOnline(deviceId)) {
                    return;
                }
                dataSender.heartBeat(deviceId, reportTime);
            } catch (Exception e) {
                log.error("心跳发送异常: {}", e.getMessage(), e);
            }
        }, 0, time + offset, TimeUnit.SECONDS);
    }

    /**
     * 判断设备是否在线
     */
    private boolean isDeviceOnline(String deviceId) {
        //先查询缓存
        String s = redisTemplate.opsForValue().get("device:OffLine:" + deviceId);
        String status = "online";
        if (s != null) {
            status = this.lambdaQuery().eq(VirtualDevice::getDeviceCode, deviceId)
                    .one().getStatus();
        }
        return status.equals("online");
    }
}
