package com.ncwu.ingestgroup.consumer;


import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.ncwu.common.Bo.ErrorDataMessageBO;
import com.ncwu.common.Bo.WaterQualityDataBo;
import com.ncwu.ingestgroup.entity.IotDeviceData;
import com.ncwu.ingestgroup.exception.DeserializationFailedException;
import com.ncwu.ingestgroup.mapper.IotDataMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private final RocketMQTemplate rocketMQTemplate;
    private InfluxDBClient influxDBClient;

    @PostConstruct
    public void init() {
        String token = System.getenv("INFLUX_TOKEN");
        influxDBClient = InfluxDBClientFactory
                .create("http://localhost:8086", token.toCharArray());
    }

    @Override
    public void onMessage(String s) {
        System.out.println(s);
        String bucket = "water";
        String org = "ncwu";
        WaterQualityDataBo waterQualityDataBo;
        try {
            //消息反序列化为对象
            waterQualityDataBo = objectMapper.readValue(s, WaterQualityDataBo.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        IotDeviceData iotDeviceData = new IotDeviceData();
        iotDeviceData.setDeviceType("WATER_QUALITY");
        iotDeviceData.setDataPayload(s);
        iotDeviceData.setDeviceCode(waterQualityDataBo.getDeviceId());
        iotDeviceData.setCollectTime(waterQualityDataBo.getTimeStamp());

        //批量写入数据库
        synchronized (this) {
            buffer.add(iotDeviceData);
            if (buffer.size() >= 200) {
                WaterQualityDataConsumer waterQualityDataConsumer = (WaterQualityDataConsumer) AopContext.currentProxy();
                waterQualityDataConsumer.saveBatch(buffer);
                buffer.clear();
            }
        }
        //检查数据状态
        if (waterQualityDataBo.getStatus().equals("error")){
            ErrorDataMessageBO errorDataMessageBO = new ErrorDataMessageBO();
            errorDataMessageBO.setDeviceId(waterQualityDataBo.getDeviceId());
            errorDataMessageBO.setPayLoad(s);
            /* 序列化 errorDataMessageBO 对象 */
            String data;
            try {
                data = objectMapper.writeValueAsString(errorDataMessageBO);
            } catch (JsonProcessingException e) {
                throw new DeserializationFailedException("序列化失败");
            }
            if (data != null) {
                rocketMQTemplate.convertAndSend("ErrorData", data);
            }
            return;
        }
        //上报到 influxdb
        ZonedDateTime zdt = waterQualityDataBo.getTimeStamp().atZone(ZoneId.of("Asia/Shanghai"));
        Point point = Point.measurement("water_quality")
                .addTag("deviceId", waterQualityDataBo.getDeviceId())
                .addField("ph", waterQualityDataBo.getPh())
                .addField("chlorine", waterQualityDataBo.getChlorine())
                .addField("turbidity", waterQualityDataBo.getTurbidity())
                .time(zdt.toInstant(), WritePrecision.MS);
        WriteApiBlocking writeApiBlocking = influxDBClient.getWriteApiBlocking();
        writeApiBlocking.writePoint(bucket, org, point);
    }
}
