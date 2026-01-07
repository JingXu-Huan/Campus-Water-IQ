package com.ncwu.ingestgroup.consumer;


import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncwu.common.Bo.MeterDataBo;
import com.ncwu.ingestgroup.entity.IotDeviceData;
import com.ncwu.ingestgroup.mapper.IotDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "Meter-Data", consumerGroup = "Meter-Data-ConsumerGroup")
public class MeterDataConsumer extends ServiceImpl<IotDataMapper,IotDeviceData> implements RocketMQListener<String> , IService<IotDeviceData> {

    private final ObjectMapper objectMapper;
    private final List<IotDeviceData> buffer = new ArrayList<>(2000);


    @Override
    public void onMessage(String s) {
        try {
            MeterDataBo meterDataBo = objectMapper.readValue(s, MeterDataBo.class);
            IotDeviceData iotDeviceData = new IotDeviceData();
            iotDeviceData.setDataPayload(s);
            iotDeviceData.setDeviceCode(meterDataBo.getDeviceId());
            iotDeviceData.setCollectTime(meterDataBo.getTimeStamp());
            iotDeviceData.setDeviceType("WATER_METER");
            synchronized (this) {
                buffer.add(iotDeviceData);
                if (buffer.size() >= 2000) {
                    MeterDataConsumer meterDataConsumer = (MeterDataConsumer) AopContext.currentProxy();
                    meterDataConsumer.saveBatch(buffer);
                    buffer.clear();
                }
            }

        } catch (JsonProcessingException e) {
            // 日志记录即可，不阻塞消费
            log.error("JSON解析失败：{}",s);
        }
    }
}

