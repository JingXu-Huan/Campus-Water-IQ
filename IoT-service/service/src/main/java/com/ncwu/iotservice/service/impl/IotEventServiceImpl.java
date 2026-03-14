package com.ncwu.iotservice.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxTable;
import com.ncwu.common.domain.vo.Result;
import com.ncwu.iotservice.entity.IotDeviceEvent;
import com.ncwu.iotservice.mapper.IoTDeviceEventMapper;
import com.ncwu.iotservice.service.IoTEventService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/20
 */
@Service
@RequiredArgsConstructor
public class IotEventServiceImpl extends ServiceImpl<IoTDeviceEventMapper, IotDeviceEvent> implements IoTEventService {

    private InfluxDBClient influxDBClient;
    /**
     * 调度器，用于管理定时任务
     */
    private ScheduledExecutorService scheduler;
    private QueryApi queryApi;

    @Value("${influx.token}")
    private String influxToken;

    private final StringRedisTemplate redisTemplate;

    @PostConstruct
    public void init() {
        influxDBClient = InfluxDBClientFactory
                .create("http://localhost:8086",
                        influxToken.toCharArray(), "ncwu", "water");
        queryApi = influxDBClient.getQueryApi();
        //虚拟线程
        scheduler = Executors.newScheduledThreadPool(30,
                Thread.ofVirtual().factory());
    }

    @Override
    public Result<List<List<String>>> getLeakingDeviceList() {
        LocalDateTime now = LocalDateTime.now();
        //是否位于目标时段
        if (now.getHour() == 23 || now.getHour() <= 5) {
            return check();
        } else {
            return Result.ok("200", "不在目标时段，暂无法检测");
        }
    }

    /**
     * 方法检测设备在此时段是否存在长时间低流量，若存在，则认为可能出现漏水
     */
    private Result<List<List<String>>> check() {
        List<List<String>> list = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            List<String> res = run(i);
            list.add(res);
        }
        return Result.ok(list);
    }

    private List<String> run(int campus) {
        Set<Object> ids = redisTemplate.opsForHash().entries("OnLineMap").keySet();
        ids.forEach(id -> {
            //todo 编写fluxQuery语句来进行查询，时间窗口是过去的30秒
            String flux = "";
            String s = id.toString();
            if (s.charAt(0) == '1' && s.substring(1, 2).equals(String.valueOf(campus))) {
                List<FluxTable> fluxTables = queryApi.query(flux);
                //todo 根据返回结果得到水流量数据
            }
        });
        return null;
    }
}
