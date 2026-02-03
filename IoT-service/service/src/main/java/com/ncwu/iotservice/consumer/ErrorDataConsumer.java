package com.ncwu.iotservice.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncwu.common.domain.Bo.ErrorDataMessageBO;
import com.ncwu.iotservice.entity.IotDeviceEvent;
import com.ncwu.iotservice.exception.DeserializationFailedException;
import com.ncwu.iotservice.mapper.IoTDeviceEventMapper;
import com.ncwu.iotservice.service.WeChatNotifyService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/24
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "ErrorData", consumerGroup = "ErrorDataGroup")
public class ErrorDataConsumer implements RocketMQListener<String> {

    private final WeChatNotifyService weChatNotifyService;
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
        //判定是否短时间内多次上报，避免未来告警风暴
        //（设备号，告警程度，描述） 可以唯一确定一条告警
        LambdaQueryWrapper<IotDeviceEvent> eq = new LambdaQueryWrapper<IotDeviceEvent>()
                .select()
                .eq(IotDeviceEvent::getDeviceCode, deviceId)
                .eq(IotDeviceEvent::getEventLevel, "WARN")
                .eq(IotDeviceEvent::getEventDesc, "设备上报数据出现异常")
                .orderByDesc(IotDeviceEvent::getEventTime)
                .last("limit 1");
        //查找最近一条告警信息
        IotDeviceEvent preEvent = ioTDeviceEventMapper.selectOne(eq);
        if (preEvent == null) {
            IotDeviceEvent iotDeviceEvent = new IotDeviceEvent();
            generateDTO(iotDeviceEvent, errorDataMessageBO);
            ioTDeviceEventMapper.insert(iotDeviceEvent);
            return;
        }
        LocalDateTime eventTime = preEvent.getEventTime();
        Long id = preEvent.getId();
        //如果当前告警事件与数据库最新数据的时差超过30分钟，则插入一条新告警，将它的 ParentId 指向上一条
        if (eventTime.isBefore(LocalDateTime.now().minusMinutes(30))) {
            IotDeviceEvent iotDeviceEvent = new IotDeviceEvent();
            generateDTO(iotDeviceEvent, errorDataMessageBO);
            //设置它的前驱
            iotDeviceEvent.setParentId(id);
            ioTDeviceEventMapper.insert(iotDeviceEvent);
        } else {
            LambdaUpdateWrapper<IotDeviceEvent> update = new LambdaUpdateWrapper<IotDeviceEvent>()
                    .eq(IotDeviceEvent::getId, id)
                    .setSql("cnt = cnt + 1");
            ioTDeviceEventMapper.update(update);
        }
    }

    private static void generateDTO(IotDeviceEvent iotDeviceEvent, ErrorDataMessageBO errorDataMessageBO) {
        iotDeviceEvent.setDeviceCode(errorDataMessageBO.getDeviceId());
        iotDeviceEvent.setDeviceType(errorDataMessageBO.getDeviceType());
        iotDeviceEvent.setEventType(errorDataMessageBO.getErrorType());
        iotDeviceEvent.setEventLevel(errorDataMessageBO.getLevel());
        iotDeviceEvent.setEventDesc(errorDataMessageBO.getDesc());
        //表示根节点
        iotDeviceEvent.setParentId(null);
        LocalDateTime now = LocalDateTime.now();
        iotDeviceEvent.setEventTime(now);
        iotDeviceEvent.setCnt(1);
    }

    /**
     * 简单的时间格式化工具
     */
    private String dateFormatter(LocalDateTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return time.format(formatter);
    }
}
