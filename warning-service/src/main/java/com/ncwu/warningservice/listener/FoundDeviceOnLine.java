package com.ncwu.warningservice.listener;


import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/5
 */
@Component
@RocketMQMessageListener(topic = "DeviceOnLine", consumerGroup = "Device-On-LineGroup")
public class FoundDeviceOnLine implements RocketMQListener<String> {

    @Override
    public void onMessage(String s) {
        System.out.println("设备：" + s + "重新上线");
    }
}
