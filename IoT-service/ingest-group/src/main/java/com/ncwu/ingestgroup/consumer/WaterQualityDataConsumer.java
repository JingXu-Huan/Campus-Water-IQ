package com.ncwu.ingestgroup.consumer;


import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncwu.common.Bo.WaterQualityDataBo;
import com.ncwu.ingestgroup.entity.IotDeviceData;
import com.ncwu.ingestgroup.mapper.IotDataMapper;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/2
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "WaterQuality-Data",          // 主题
        consumerGroup = "WaterQuality-Data-ConsumerGroup" // 消费者组
)
public class WaterQualityDataConsumer extends ServiceImpl<IotDataMapper, IotDeviceData> implements RocketMQListener<String>, IService<IotDeviceData> {

    private final ObjectMapper objectMapper;
    private final List<IotDeviceData> buffer = new ArrayList<>(2000);

    @Override
    public void onMessage(String s) {
        try {
            WaterQualityDataBo waterQualityDataBo = objectMapper.readValue(s, WaterQualityDataBo.class);
            IotDeviceData iotDeviceData = new IotDeviceData();
            iotDeviceData.setDeviceType("WATER_QUALITY");
            iotDeviceData.setDataPayload(s);
            iotDeviceData.setDeviceCode(waterQualityDataBo.getDeviceId());
            iotDeviceData.setCollectTime(waterQualityDataBo.getTimeStamp());
            synchronized (this) {
                buffer.add(iotDeviceData);
                if (buffer.size() >= 2000) {
                    WaterQualityDataConsumer waterQualityDataConsumer = (WaterQualityDataConsumer) AopContext.currentProxy();
                    waterQualityDataConsumer.saveBatch(buffer);
                    buffer.clear();
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
