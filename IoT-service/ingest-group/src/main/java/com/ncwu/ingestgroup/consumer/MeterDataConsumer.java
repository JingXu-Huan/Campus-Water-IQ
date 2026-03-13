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
import com.ncwu.common.domain.Bo.ErrorDataMessageBO;
import com.ncwu.common.domain.Bo.MeterDataBo;
import com.ncwu.ingestgroup.entity.IotDeviceData;
import com.ncwu.ingestgroup.exception.DeserializationFailedException;
import com.ncwu.ingestgroup.mapper.IotDataMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/2
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "Meter-Data", consumerGroup = "Meter-Data-ConsumerGroup")
public class MeterDataConsumer extends ServiceImpl<IotDataMapper, IotDeviceData> implements
        RocketMQListener<String>, IService<IotDeviceData> {

    @Value("${influx.token}")
    private String influxToken;
    int N = 2000;
    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    private final List<IotDeviceData> buffer = new ArrayList<>(N);
    private InfluxDBClient influxDBClient;
    private WriteApiBlocking writeApi;

    private final List<Point> points = new ArrayList<>(5000);
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();

    @PostConstruct
    public void init() {
        influxDBClient = InfluxDBClientFactory
                .create("http://localhost:8086", influxToken.toCharArray(), "ncwu", "water");
        writeApi = influxDBClient.getWriteApiBlocking();
        startFlushRemainingData();
    }

    /**
     * 守护线程，每5s跑一次任务
     */
    private void startFlushRemainingData() {
        try (ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "reportDataThread");
            thread.setDaemon(true);
            return thread;
        })) {
            executorService.scheduleAtFixedRate(() -> {
                if (!points.isEmpty()) {
                    writeApi.writePoints(points);
                }
            }, 5, 5, TimeUnit.SECONDS);
        }
    }

    @PreDestroy
    public void flushRemainingData() {
        writeApi.writePoints(points);
        points.clear();
    }

    @Override
    public void onMessage(String s) {
        MeterDataBo meterDataBo;
        try {
            meterDataBo = objectMapper.readValue(s, MeterDataBo.class);
        } catch (JsonProcessingException e) {
            log.error("反序列化失败");
            throw new DeserializationFailedException("反序列化失败");
        }

        IotDeviceData iotDeviceData = new IotDeviceData();
        iotDeviceData.setDataPayload(s);
        iotDeviceData.setDeviceCode(meterDataBo.getDeviceId());
        iotDeviceData.setCollectTime(meterDataBo.getTimeStamp());
        iotDeviceData.setDeviceType("WATER_METER");
        //批量保存原始数据到数据库
        synchronized (lock1) {
            buffer.add(iotDeviceData);
            if (buffer.size() >= N) {
                MeterDataConsumer meterDataConsumer = (MeterDataConsumer) AopContext.currentProxy();
                meterDataConsumer.saveBatch(buffer);
                buffer.clear();
            }
        }
        //检查数据状态
        if (meterDataBo.getStatus().equals("error")) {
            generateAndSendErrorBO(s, meterDataBo);
            //异常数据不送时序数据库
            return;
        } else if (meterDataBo.getStatus().equals("burstPipe")) {
            String data;
            try {
                data = objectMapper.writeValueAsString(meterDataBo);
            } catch (JsonProcessingException e) {
                throw new DeserializationFailedException("序列化失败");
            }
            String deviceId = meterDataBo.getDeviceId();
            ErrorDataMessageBO errorDataMessageBO = new ErrorDataMessageBO();

            errorDataMessageBO.setDeviceId(deviceId);
            errorDataMessageBO.setLevel("CRITICAL");
            errorDataMessageBO.setErrorType("THRESHOLD");
            errorDataMessageBO.setDeviceType("METER");
            errorDataMessageBO.setDesc("设备" + deviceId + "可能发生爆管事件。");
            errorDataMessageBO.setPayLoad(data);
//            errorDataMessageBO.setSuggestion("请检查管网是否有破损。");
            rocketMQTemplate.convertAndSend("ErrorData", errorDataMessageBO);
            //异常数据不送时序数据库
            return;
        }
        //正常数据：送到influxdb
        ZonedDateTime zdt = meterDataBo.getTimeStamp().atZone(ZoneId.of("Asia/Shanghai"));
        synchronized (lock2) {
            points.add(Point
                    .measurement("water_meter")
                    .addTag("deviceId", meterDataBo.getDeviceId())
                    .addField("flow", meterDataBo.getFlow())
                    .addField("usage", meterDataBo.getTotalUsage())
                    .addField("tem", meterDataBo.getWaterTem())
                    .addField("pressure", meterDataBo.getPressure())
                    .time(zdt.toInstant(), WritePrecision.MS));
            if (points.size() >= 500) {
                writeApi.writePoints(points);
                points.clear();
            }
        }
    }

    private void generateAndSendErrorBO(String s, MeterDataBo meterDataBo) {
        String deviceId = meterDataBo.getDeviceId();
        ErrorDataMessageBO errorDataMessageBO = new ErrorDataMessageBO();
        errorDataMessageBO.setDesc("设备" + deviceId + "数据异常");
        errorDataMessageBO.setErrorType("ABNORMAL");
        errorDataMessageBO.setDeviceId(deviceId);
        errorDataMessageBO.setPayLoad(s);
        errorDataMessageBO.setDeviceType("METER");
        errorDataMessageBO.setLevel("WARN");
//        errorDataMessageBO.setSuggestion("请断电重启，并且检查传感器是否正常工作。");
        rocketMQTemplate.convertAndSend("ErrorData", errorDataMessageBO);
    }

}

