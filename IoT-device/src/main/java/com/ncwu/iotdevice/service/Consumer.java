package com.ncwu.iotdevice.service;


import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ncwu.iotdevice.domain.entity.VirtualDevice;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/8
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "OpsForDataBase", consumerGroup = "OpsForDataBase")
public class Consumer implements RocketMQListener<String> {

    private final VirtualMeterDeviceService meterDeviceService;
    private final VirtualWaterQualityDeviceService waterQualityDeviceService;


    @Override
    public void onMessage(String s) {
        LambdaUpdateWrapper<VirtualDevice> updateWrapper = new LambdaUpdateWrapper<VirtualDevice>()
                .eq(VirtualDevice::getIsRunning, true)
                .set(VirtualDevice::getIsRunning, false);
        if (s.equals("LetAllMetersStopRunning")) {
            meterDeviceService.update(updateWrapper);
        } else if (s.equals("LetAllWaterQualitiesStopRunning")) {
            waterQualityDeviceService.update(updateWrapper);
        } else {
            return;
        }
    }
}
