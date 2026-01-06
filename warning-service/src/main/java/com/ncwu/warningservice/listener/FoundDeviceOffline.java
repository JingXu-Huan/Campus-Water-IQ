package com.ncwu.warningservice.listener;


import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/6
 */
@Component
@RocketMQMessageListener(topic = "DeviceOffline", consumerGroup = "Device-Off-LineGroup")
public class FoundDeviceOffline implements RocketMQListener<String> {
    @Override
    public void onMessage(String s) {
        System.out.println("检测到设备" + s + "下线");
    }
}
