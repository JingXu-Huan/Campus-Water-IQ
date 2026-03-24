package com.ncwu.iotservice.schedule;


import com.influxdb.client.QueryApi;
import com.ncwu.common.apis.warning_service.EmailServiceInterFace;
import com.ncwu.common.apis.warning_service.WeChatNotifyInterFace;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckHighFlow {
    private final StringRedisTemplate redisTemplate;
    private final QueryApi queryApi;

    @DubboReference(version = "1.0.0", interfaceClass = EmailServiceInterFace.class)
    private EmailServiceInterFace emailServiceInterFace;
    @DubboReference(version = "1.0.0", interfaceClass = WeChatNotifyInterFace.class)
    private WeChatNotifyInterFace weChatNotifyInterFace;

    private final ExecutorService pool = Executors.newFixedThreadPool(
            20,
            Thread.ofVirtual().name("high-flow-vt-", 0).factory()
    );

    List<String> meters;

    @PostConstruct
    public void init() {
        meters = redisTemplate.opsForHash()
                .entries("OnLineMap").keySet().stream().map(Object::toString)
                .filter((deviceId) -> deviceId.startsWith("1")).toList();
    }

    //todo 定时任务检测是否存在高流量异常 高流量异常定义：单位时间内流量超过某个阈值 需要根据实际情况设置阈值
    @Scheduled(fixedDelay = 1000)
    public void checkHighFlow() {
        int i = Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get("isInit")));
        if (i == 1) {
            log.info("定时任务检查高流量异常");
            //todo 拉离线设备列表，排除离线设备
            meters.forEach(id -> {
                Double flow = queryApi.query(String.format("""
                        from(bucket: "water")
                        |> range(start: -15s)
                        |> filter(fn: (r) =>
                                r._measurement == "water_meter" and
                                r._field == "flow" and
                                r.deviceId == "%s"
                           )
                        |> last()
                        |> keep(columns: ["_time", "_value"])
                        """, id)).stream().flatMap(t -> t.getRecords().stream()).map(r -> r.getValue() == null ?
                        0.0 : Double.parseDouble(r.getValue().toString())).findFirst().orElse(0.0);
                if (flow >= 0.45) {
                    log.warn("设备{}存在高流量异常，当前流量为{}", id, flow);
                    //查询设备绑定的用户邮箱，给用户和管理员发送邮件通知
                    pool.submit(() -> {
                        //微信机器人通知
                        weChatNotifyInterFace.sendText("设备" + id + "存在高流量异常，当前流量为" + flow);
                    });
                }

            });
        }

    }

}
