package com.ncwu.warningservice.listener;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncwu.warningservice.exception.DeserializationFailedException;
import com.ncwu.warningservice.wechatservice.WeChatNotifyService;
import com.ncwu.common.domain.dto.IoTDeviceEventDTO;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/2
 */
@Component
@RocketMQMessageListener(topic = "IoTDeviceEvent", consumerGroup = "IoTDeviceEventGroup")
@RequiredArgsConstructor
public class FoundErrorEvent implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;

    private final WeChatNotifyService weChatNotifyService;


    IoTDeviceEventDTO ioTDeviceEventDTO;

    @Override
    public void onMessage(String s) {
        try {
            ioTDeviceEventDTO = objectMapper.readValue(s,
                    IoTDeviceEventDTO.class);
        } catch (JsonProcessingException e) {
            throw new DeserializationFailedException("反序列化失败");
        }
    }
}
