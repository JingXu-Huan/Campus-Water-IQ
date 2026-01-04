package com.ncwu.iotservice.service.impl;


import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/2
 */
@Component
@RocketMQMessageListener(
        topic = "WaterQuality-Data",          // 主题
        consumerGroup = "WaterQuality-Data-ConsumerGroup" // 消费者组
)
public class WaterQualityDataConsumer implements RocketMQListener<String> {
    @Override
    public void onMessage(String s) {
        System.out.println(s);
    }
}
