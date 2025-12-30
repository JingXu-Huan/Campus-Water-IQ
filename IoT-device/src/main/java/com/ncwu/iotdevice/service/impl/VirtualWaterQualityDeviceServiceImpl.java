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

    //开大小为5的线程池
    final ExecutorService pool = Executors.newFixedThreadPool(5);

    //设备是否已经完成了初始化
    @Setter
    public volatile boolean isInit = false;
    //上报任务是否正在进行
    public volatile boolean isRunning = false;

    private ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> deviceTasks = new ConcurrentHashMap<>();

    private final DeviceMapper deviceMapper;
    private final StringRedisTemplate redisTemplate;
    private final ServerConfig serverConfig;

    @PostConstruct
    void init() {
        Long size = redisTemplate.opsForSet().size("device:sensor");
        if (size == null) {
            throw new DeviceRegisterException("请先初始化设备");
        }
        scheduler = new ScheduledThreadPoolExecutor(Math.max(1, (int) (size / 100L)));
    }

    @Override
    public Result<String> startAll() {
        if (!isInit) {
            return Result.fail(ErrorCode.BUSINESS_ERROR.code(), ErrorCode.BUSINESS_INIT_ERROR.message());
        }
        if (isRunning) {
            return Result.fail(ErrorCode.BUSINESS_DEVICE_RUNNING_NOW_ERROR.code(),
                    ErrorCode.BUSINESS_DEVICE_RUNNING_NOW_ERROR.message());
        }
        Set<String> ids = redisTemplate.opsForSet().members("device:sensor");
        if (ids == null || ids.isEmpty()) {
            return Result.fail(ErrorCode.BUSINESS_ERROR.code(), ErrorCode.BUSINESS_INIT_ERROR.message());
        }
        pool.submit(() -> {
            //修改数据库状态
            LambdaUpdateChainWrapper<VirtualDevice> wrapper = this.lambdaUpdate()
                    .in(VirtualDevice::getId, ids)
                    .eq(VirtualDevice::getStatus, "offline")
                    .set(VirtualDevice::getStatus, "online");
            this.update(wrapper);
        });
        ids.forEach(this::scheduleNextReport);
        log.info("成功开启{}台设备的数据流", ids.size());
        this.isRunning = true;
        return Result.ok(null, SuccessCode.DEVICE_OPEN_SUCCESS.getCode(),
                SuccessCode.DEVICE_OPEN_SUCCESS.getMessage()
        );
    }

    @Override
    public Result<String> startList(List<String> ids) {
        return null;
    }

    private void scheduleNextReport(String deviceId) {
        //读取上报时间
        String reportFrequency = serverConfig.getReportFrequency();
        String timeOffset = serverConfig.getTimeOffset();
        if (reportFrequency == null) {
            return;
        }
        long delay = Long.parseLong(reportFrequency) + ThreadLocalRandom.current()
                .nextLong(Long.parseLong(timeOffset));
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                processSingleDevice(deviceId);
                scheduleNextReport(deviceId);
            } catch (Exception e) {
                log.error("设备:{}上报失败,原因:{}", deviceId, e.getMessage());
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

        //todo 接入消息队列
        System.out.println(dataBo);
    }

}
