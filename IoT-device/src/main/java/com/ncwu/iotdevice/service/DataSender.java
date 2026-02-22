package com.ncwu.iotdevice.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncwu.iotdevice.AOP.annotation.CloseValue;
import com.ncwu.iotdevice.AOP.annotation.NotCredible;
import com.ncwu.iotdevice.AOP.annotation.RandomEvent;
import com.ncwu.iotdevice.domain.Bo.MeterDataBo;
import com.ncwu.iotdevice.domain.Bo.WaterQualityDataBo;
import com.ncwu.iotdevice.exception.MessageSendException;
import com.ncwu.iotdevice.mapper.DeviceMapper;
import com.ncwu.iotdevice.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.Counter;

import java.time.ZoneId;

import static com.ncwu.iotdevice.utils.Utils.keep3;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/1
 */
@Slf4j
@Service
public class DataSender {

    //两个消息队列消费计数器，用于监控失败比例
    private final Counter messageSuccessCounter;
    private final Counter messageFailureCounter;
    private final ObjectMapper objectMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final StringRedisTemplate redisTemplate;
    private final DeviceMapper deviceMapper;
    private final VirtualMeterDeviceService virtualMeterDeviceService;

    //构造函数，用于自动装配
    public DataSender(@Qualifier("messageSuccessCounter") Counter MessageSuccessCounter,
                      @Qualifier("messageFailureCounter") Counter MessageFailureCounter,
                      ObjectMapper objectMapper,
                      RocketMQTemplate rocketMQTemplate,
                      StringRedisTemplate redisTemplate,
                      DeviceMapper deviceMapper,
                      VirtualMeterDeviceService virtualMeterDeviceService
    ) {
        this.messageSuccessCounter = MessageSuccessCounter;
        this.messageFailureCounter = MessageFailureCounter;
        this.objectMapper = objectMapper;
        this.rocketMQTemplate = rocketMQTemplate;
        this.redisTemplate = redisTemplate;
        this.deviceMapper = deviceMapper;
        this.virtualMeterDeviceService = virtualMeterDeviceService;
    }


    /**
     * 此方法传递发送的数据，并且更新 redis 中设备的在在线状态
     *
     * @param dataBo 数据载荷
     * @throws MessageSendException 数据发送失败异常
     */
    @NotCredible
    @RandomEvent
    @CloseValue
    public void sendMeterData(MeterDataBo dataBo) throws MessageSendException {
        //检查上一次上报时间
        String deviceId = dataBo.getDeviceId();
        Object onLineMap = redisTemplate.opsForHash().get("OnLineMap", dataBo.getDeviceId());
        long deviceCurrentTime = dataBo.getTimeStamp().atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
        long now = System.currentTimeMillis();
        if (onLineMap != null) {
            long preTime = Long.parseLong(String.valueOf(onLineMap));
            if (preTime == -1) {
                heartBeat(deviceId, now);
                return;
            }
            if (deviceCurrentTime <= preTime) {
                log.warn("检测到重复数据，跳过上报{}", deviceId);
                return;
            }
            double increment = keep3(dataBo.getFlow() * (deviceCurrentTime - preTime) / 1000.0);

            Double currentTotal = redisTemplate.opsForHash().increment("meter:total_usage", deviceId, increment);
            dataBo.setTotalUsage(keep3(currentTotal));
        }
        Boolean onLine = redisTemplate.hasKey("device:OffLine:" + deviceId);
        if (onLine) {
            // 消息队列通知上线
            try {
                rocketMQTemplate.convertAndSend("DeviceOnLine", deviceId);
                messageSuccessCounter.increment();
            } catch (Exception e) {
                log.error("设备上线通知发送失败: deviceId={}, error={}", deviceId, e.getMessage());
                messageFailureCounter.increment();
            }
            //如果设备上线,调用设备上线后置处理器
            virtualMeterDeviceService.markDeviceOnline(deviceId, now, deviceMapper, redisTemplate);
        }
        String data;
        try {
            data = objectMapper.writeValueAsString(dataBo);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        //Iot设备上报数据频率高，这里使用异步调用
        asyncSendData("Meter-Data", data);
    }

    public void heartBeat(String deviceId, long timestamp) {
        redisTemplate.opsForHash().put("OnLineMap", deviceId, String.valueOf(timestamp));
    }

    public void sendWaterQualityData(WaterQualityDataBo dataBo) throws MessageSendException {
        String deviceId = dataBo.getDeviceId();
        long timestamp = System.currentTimeMillis();
        //每次接到上报的数据，就查询redis的离线列表，看看有没有离线设备重新上报数据(重新上线)
        Boolean onLine = redisTemplate.hasKey("device:OffLine:" + deviceId);
        if (onLine) {
            //消息队列通知上线
            try {
                rocketMQTemplate.convertAndSend("DeviceOnLine", deviceId);
                messageSuccessCounter.increment();
            } catch (Exception e) {
                log.error("设备上线通知发送失败: deviceId={}, error={}", deviceId, e.getMessage());
                messageFailureCounter.increment();
            }
            //如果设备上线,调用设备上线后置处理器
            virtualMeterDeviceService.markDeviceOnline(deviceId, timestamp, deviceMapper, redisTemplate);
        }
        String data;
        try {
            data = objectMapper.writeValueAsString(dataBo);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        //Iot设备上报数据频率高，这里使用异步调用
        asyncSendData("WaterQuality-Data", data);
    }

    /**
     * 异步向 mq 发送消息
     * <p>
     * 失败将重新发送一次，随后再失败将记录日志
     */
    private <T> void asyncSendData(String topic, T data) {
        rocketMQTemplate.asyncSend(topic, data, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                messageSuccessCounter.increment();
            }

            @Override
            public void onException(Throwable throwable) {
                messageFailureCounter.increment();
                log.error("MQ 异步发送失败，尝试再次异步发送", throwable);
                rocketMQTemplate.asyncSend(topic, data, new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.info("重试发送成功");
                        messageSuccessCounter.increment();
                    }

                    @Override
                    public void onException(Throwable t) {
                        messageFailureCounter.increment();
                        log.error("重试发送仍然失败，记录{}", data);
                    }
                });
            }
        });
    }
}


