package com.ncwu.iotdevice.service;


import com.ncwu.iotdevice.AOP.annotation.NotCredible;
import com.ncwu.iotdevice.AOP.annotation.RandomEvent;
import com.ncwu.iotdevice.config.ServerConfig;
import com.ncwu.iotdevice.domain.Bo.MeterDataBo;
import com.ncwu.iotdevice.exception.MessageSendException;
import com.ncwu.iotdevice.mapper.DeviceMapper;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import static com.ncwu.iotdevice.utils.Utils.keep3;
import static com.ncwu.iotdevice.utils.Utils.markDeviceOnline;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/1
 */
@Service
@RequiredArgsConstructor
public class DataSender {

    private final RocketMQTemplate rocketMQTemplate;
    private final StringRedisTemplate redisTemplate;
    private final DeviceMapper deviceMapper;
    private final ServerConfig serverConfig;

    /**
     * 此方法传递发送的数据，并且更新 redis 中设备的在在线状态
     *
     * @param dataBo 数据载荷
     * @throws MessageSendException 数据发送失败异常
     */
    @NotCredible
    @RandomEvent
    public void sendData(MeterDataBo dataBo) throws MessageSendException {
        // 模拟发送，实际可接入消息队列
        //log.debug("上报数据: {}", dataBo);
        //更新 redis 中时间戳,就是向 redis 上报自己的心跳
        //获取当前系统时间
        String deviceId = dataBo.getDeviceId();
        long timestamp = System.currentTimeMillis();
        heartBeat(deviceId, timestamp);
        //todo 消息队列通知上线

        Boolean onLine = redisTemplate.hasKey("device:OffLine:" + deviceId);
        if (onLine) {
            //如果设备上线,调用设备上线后置处理器
            markDeviceOnline(deviceId, timestamp, deviceMapper, redisTemplate);
        }
        double reportFrequency;
        try {
            reportFrequency = Double.parseDouble(serverConfig.getReportFrequency());
        } catch (NumberFormatException e) {
            throw new MessageSendException("Invalid reportFrequency configuration: " + serverConfig.getReportFrequency(), e);
        }
        double increment = keep3(dataBo.getFlow() * reportFrequency / 1000);
        Double currentTotal = redisTemplate.opsForHash().increment("meter:total_usage", deviceId, increment);
        dataBo.setTotalUsage(keep3(currentTotal));
        rocketMQTemplate.convertAndSend("Meter-Data", dataBo);
    }

    public void heartBeat(String deviceId, long timestamp) {
        redisTemplate.opsForHash().put("OnLineMap", deviceId, String.valueOf(timestamp));
    }
}
