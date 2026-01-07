package com.ncwu.iotservice.service.impl;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncwu.common.Bo.MeterDataBo;
import com.ncwu.iotservice.entity.IotDeviceData;
import com.ncwu.iotservice.service.IoTDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
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
public class MeterDataConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;
    private final List<IotDeviceData> buffer = new ArrayList<>();

    private final IoTDataService ioTDataService;

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
                int BATCH_SIZE = 2000;
                if (buffer.size() >= BATCH_SIZE) {
                    ioTDataService.saveBatch(new ArrayList<>(buffer));
                    buffer.clear();
                }
            }

        } catch (JsonProcessingException e) {
            // 日志记录即可，不阻塞消费
            log.error("JSON解析失败：{}",s);
        }
    }
}

