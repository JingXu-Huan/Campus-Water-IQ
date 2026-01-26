package com.ncwu.warningservice.listener;


import com.ncwu.warningservice.wechatservice.WeChatNotifyService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/5
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "DeviceOnLine", consumerGroup = "Device-On-LineGroup")
public class FoundDeviceOnLine implements RocketMQListener<String> {
    private final WeChatNotifyService weChatNotifyService;
    @Override
    public void onMessage(String s) {
        weChatNotifyService.sendText("设备：" + s + "重新上线");
        System.out.println("设备：" + s + "重新上线");
    }
}
