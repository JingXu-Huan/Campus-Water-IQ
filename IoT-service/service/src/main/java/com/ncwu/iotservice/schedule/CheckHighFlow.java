package com.ncwu.iotservice.schedule;


import com.influxdb.client.QueryApi;
import com.ncwu.common.apis.warning_service.EmailServiceInterFace;
import com.ncwu.common.apis.warning_service.EventInterFace;
import com.ncwu.common.apis.warning_service.WeChatNotifyInterFace;
import com.ncwu.common.domain.IotDeviceEvent;
import com.ncwu.common.domain.vo.Result;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
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
    @DubboReference(version = "1.0.0", interfaceClass = EventInterFace.class)
    private EventInterFace eventInterFace;

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
    @Scheduled(fixedDelay = 3000)
    public void checkHighFlow() {
        String isInit = redisTemplate.opsForValue().get("isInit");
        if (isInit == null || Integer.parseInt(isInit) == 0) {
            return;
        }
        if (Integer.parseInt(isInit) == 1) {
            log.info("定时任务---开始检测高流量");
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
                                """, id)).stream().flatMap(t -> t.getRecords().stream())
                        .map(r -> r.getValue() == null ?
                                0.0 : Double.parseDouble(r.getValue().toString())).findFirst().orElse(0.0);
                if (flow >= 0.25) {
                    log.warn("设备{}存在高流量异常，当前流量为{}", id, flow);
                    //todo 查询设备绑定的用户邮箱，给用户和管理员发送邮件通知
                    pool.submit(() -> {
                        // 不让任一通知失败影响事件入库
                        addEvent(id, flow);
                        try {
                            //微信机器人通知
                            weChatNotifyInterFace.sendText("设备" + id + "存在高流量异常，当前流量为" + flow);
                        } catch (Exception e) {
                            log.error("发送高流量异常微信通知失败, deviceId={}", id, e);
                        }
                        try {
                            emailServiceInterFace.sendMail("水务物联网平台高流量异常通知",
                                    "设备" + id + "存在高流量异常，当前流量为" + flow, "****@gmail.com");
                        } catch (MessagingException e) {
                            log.error("发送高流量异常邮件通知失败", e);
                        }
                    });
                }
            });
        }

    }

    private void addEvent(String id, Double flow) {
        try {
            IotDeviceEvent iotDeviceEvent = new IotDeviceEvent();
            LocalDateTime now = LocalDateTime.now();
            iotDeviceEvent.setCreateTime(now);
            iotDeviceEvent.setEventTime(now);
            iotDeviceEvent.setEventType("THRESHOLD");
            iotDeviceEvent.setDeviceCode(id);
            iotDeviceEvent.setDeviceType("METER");
            iotDeviceEvent.setEventDesc("设备" + id + "存在高流量异常，当前流量为" + flow);
            iotDeviceEvent.setEventLevel("WARNING");
            iotDeviceEvent.setHandledFlag(0);

            Result<Boolean> result = eventInterFace.addNewEvent(iotDeviceEvent);
            log.info("高流量事件上报完成, deviceId={}, flow={}, result={}", id, flow, result);
        } catch (Exception e) {
            log.error("高流量事件上报失败, deviceId={}, flow={}", id, flow, e);
        }
    }

}
