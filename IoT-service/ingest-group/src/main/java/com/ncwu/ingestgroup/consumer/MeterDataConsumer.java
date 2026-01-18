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
import com.ncwu.common.Bo.MeterDataBo;
import com.ncwu.ingestgroup.entity.IotDeviceData;
import com.ncwu.ingestgroup.mapper.IotDataMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
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
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "Meter-Data", consumerGroup = "Meter-Data-ConsumerGroup")
public class MeterDataConsumer extends ServiceImpl<IotDataMapper, IotDeviceData> implements RocketMQListener<String>, IService<IotDeviceData> {

    private final ObjectMapper objectMapper;
    private final List<IotDeviceData> buffer = new ArrayList<>(2000);
    private InfluxDBClient influxDBClient;

    @PostConstruct
    public void init() {
        String token = System.getenv("INFLUX_TOKEN");
        influxDBClient = InfluxDBClientFactory
                .create("http://localhost:8086", token.toCharArray());
    }

    @Override
    public void onMessage(String s) {
        String bucket = "test08";
        String org = "jingxu";
        System.out.println(s);
        try {
            MeterDataBo meterDataBo = objectMapper.readValue(s, MeterDataBo.class);
            if (meterDataBo.getStatus().equals("error")) {
                //todo 这里以后要自行检测数据状态，检查取值范围。
                return;
            }
            IotDeviceData iotDeviceData = new IotDeviceData();
            iotDeviceData.setDataPayload(s);
            iotDeviceData.setDeviceCode(meterDataBo.getDeviceId());
            iotDeviceData.setCollectTime(meterDataBo.getTimeStamp());
            iotDeviceData.setDeviceType("WATER_METER");

            //流量，送到influxdb
            ZonedDateTime zdt = meterDataBo.getTimeStamp().atZone(ZoneId.of("Asia/Shanghai"));
            Point point = Point
                    .measurement(meterDataBo.getDeviceId())
                    .addField("flow", meterDataBo.getFlow())
                    .addField("usage", meterDataBo.getTotalUsage())
                    .addField("tem", meterDataBo.getWaterTem())
                    .time(zdt.toInstant(), WritePrecision.MS);

            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            writeApi.writePoint(bucket, org, point);
            synchronized (this) {
                buffer.add(iotDeviceData);
                if (buffer.size() >= 200) {
                    MeterDataConsumer meterDataConsumer = (MeterDataConsumer) AopContext.currentProxy();
                    meterDataConsumer.saveBatch(buffer);
                    buffer.clear();
                }
            }

        } catch (JsonProcessingException e) {
            // 日志记录即可，不阻塞消费
            log.error("JSON解析失败：{}", s);
        }
    }

}

