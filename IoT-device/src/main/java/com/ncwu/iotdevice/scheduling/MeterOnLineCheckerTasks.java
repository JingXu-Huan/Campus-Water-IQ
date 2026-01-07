package com.ncwu.iotdevice.scheduling;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ncwu.iotdevice.config.ServerConfig;
import com.ncwu.iotdevice.domain.entity.VirtualDevice;
import com.ncwu.iotdevice.mapper.DeviceMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * 定时任务类
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/27
 */
@Getter
@Slf4j
@Component
@RequiredArgsConstructor
public class MeterOnLineCheckerTasks {

    ExecutorService pool = Executors.newFixedThreadPool(7);

    private final StringRedisTemplate redisTemplate;
    private final DeviceMapper deviceMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final ServerConfig serverConfig;

    @Scheduled(fixedDelay = 30 * 1000)
    public void checkOnLineDevices() {
//        检查设备运行控制器
        long meterChecked = Long.parseLong(Objects.requireNonNull(redisTemplate.opsForValue().get("MeterChecked")));

        if (meterChecked == 0) {
            return;
        }
        log.warn("水表--开始检测");
        String prefix = "OnLineMap";
        //获取当前系统时间戳
        long now = System.currentTimeMillis();
        //按量扫描，避免数据量过大产生OOM
        ScanOptions options = ScanOptions.scanOptions().match("*").count(15000).build();
        try (Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash().scan(prefix, options)) {
            while (cursor.hasNext()) {
                Map.Entry<Object, Object> entry = cursor.next();
                String deviceId = (String) entry.getKey();
                long lastReportTime = Long.parseLong(entry.getValue().toString());
                //排除未启动的设备和水质传感器
                if (lastReportTime == -1 || deviceId.startsWith("2")) {
                    continue;
                }
                long i = Long.parseLong(serverConfig.getWaterQualityReportFrequency());
                int n = Math.max(1, serverConfig.getN());
                if (now - lastReportTime > i * n) {
                    processOffline(deviceId);
                }
            }
        } catch (Exception e) {
            log.error("设备下线扫描异常", e);
        }

    }

    /**
     * 设备下线后置处理器
     * <p>
     * 主要是修改数据库状态,并且通知告警服务。
     *
     * @param deviceId 设备编号
     *
     */
    private void processOffline(String deviceId) {
        //发送下线消息
        rocketMQTemplate.convertAndSend("DeviceOffline", deviceId);
        //更新数据库状态,(此时可能缓存中还有此设备的在线信息,也要一并删除)
        pool.submit(() -> {
            LambdaUpdateWrapper<VirtualDevice> updateWrapper = new LambdaUpdateWrapper<VirtualDevice>()
                    .eq(VirtualDevice::getDeviceCode, deviceId)
                    .set(VirtualDevice::getStatus, "offline");
            deviceMapper.update(updateWrapper);
        });
        redisTemplate.delete("cache:meter:status:" + deviceId);
        //在线状态表也要一并清除
        redisTemplate.opsForHash().delete("OnLineMap", deviceId);
        //在 redis 维护下线缓存列表,为设备后续上线提供方便
        redisTemplate.opsForValue().set("device:OffLine:" + deviceId, "offLine", 7, TimeUnit.DAYS);
    }

}

