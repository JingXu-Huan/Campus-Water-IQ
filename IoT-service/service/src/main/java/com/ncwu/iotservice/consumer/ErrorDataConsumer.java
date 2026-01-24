package com.ncwu.iotservice.consumer;


import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncwu.common.Bo.ErrorDataMessageBO;
import com.ncwu.iotservice.entity.IotDeviceEvent;
import com.ncwu.iotservice.exception.DeserializationFailedException;
import com.ncwu.iotservice.mapper.IoTDeviceEventMapper;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/24
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "ErrorData", consumerGroup = "ErrorDataGroup")
public class ErrorDataConsumer implements RocketMQListener<String> {

    private final IoTDeviceEventMapper ioTDeviceEventMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(String s) {
        ErrorDataMessageBO errorDataMessageBO;
        try {
            errorDataMessageBO = objectMapper.readValue(s, ErrorDataMessageBO.class);
        } catch (JsonProcessingException e) {
            throw new DeserializationFailedException("反序列化失败");
        }
        String deviceId = errorDataMessageBO.getDeviceId();
        IotDeviceEvent iotDeviceEvent = new IotDeviceEvent();
        generateDTO(iotDeviceEvent, deviceId, "WARN");
        //判定是否短时间内多次上报，避免未来告警风暴
        LambdaQueryWrapper<IotDeviceEvent> eq = new LambdaQueryWrapper<IotDeviceEvent>().select()
                .eq(IotDeviceEvent::getDeviceCode, deviceId)
                .eq(IotDeviceEvent::getEventLevel, "WARN")
                .eq(IotDeviceEvent::getEventDesc, "设备上报数据出现异常")
                .orderByDesc(IotDeviceEvent::getEventTime);
        LocalDateTime eventTime = ioTDeviceEventMapper.selectOne(eq).getEventTime();

        if (eventTime.minusMinutes(30).isAfter(LocalDateTime.now())) {
            iotDeviceEvent.setCreateTime(LocalDateTime.now());
            ioTDeviceEventMapper.insert(iotDeviceEvent);
        }
    }

    private static void generateDTO(IotDeviceEvent iotDeviceEvent, String deviceId, String level) {
        iotDeviceEvent.setDeviceCode(deviceId);
        iotDeviceEvent.setDeviceType("METER");
        iotDeviceEvent.setEventType("ABNORMAL");
        iotDeviceEvent.setEventLevel(level);
        iotDeviceEvent.setEventDesc("设备上报数据出现异常");
        LocalDateTime now = LocalDateTime.now();
        iotDeviceEvent.setEventTime(now);
    }
}
