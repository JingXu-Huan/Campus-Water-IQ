package com.ncwu.iotdevice.AOP.Aspects;


import com.ncwu.iotdevice.AOP.annotation.RandomEvent;
import com.ncwu.iotdevice.config.ServerConfig;
import com.ncwu.iotdevice.domain.Bo.MeterDataBo;
import io.netty.util.internal.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.ncwu.iotdevice.utils.Utils.keep3;
import static com.ncwu.iotdevice.utils.Utils.waterPressureGenerate;

/**
 * 控制异常行为的 AOP 切面类
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/1
 */
@Aspect
@Component
@RequiredArgsConstructor
public class ChangeTheData {
    private final StringRedisTemplate redisTemplate;
    private final ServerConfig serverConfig;
    Set<String> set = ConcurrentHashMap.newKeySet();

    @Order(2)
    @Around("@annotation(randomEvent)")
    public Object giveEvent(ProceedingJoinPoint pjp, RandomEvent randomEvent) throws Throwable {
        String mode = redisTemplate.opsForValue().get("mode");
        Long size = redisTemplate.opsForSet().size("device:meter");

        if (size == null) {
            return null;
        }
        //正常模式
        if (mode == null || "normal".equals(mode)) {
            return pjp.proceed();
        }
        //爆管
        if ("burstPipe".equals(mode)) {
            MeterDataBo dataBo = getMeterDataBo(pjp);
            if (set.size() < Math.max(1, size * 0.05)) {
                set.add(dataBo.getDeviceId());
            }
            if (set.contains(dataBo.getDeviceId())) {
                MeterDataBo modifiedData = preburstPipeProcessor(pjp);
                return pjp.proceed(new Object[]{modifiedData});
            }
        }
        //漏水
        if ("leaking".equals(mode)) {
            MeterDataBo modifiedData = preLeakingProcessor(pjp);
            return pjp.proceed(new Object[]{modifiedData});
        }
        if ("shows".equals(mode)) {
            MeterDataBo modifiedData = preShowsProcessor(pjp);
            return pjp.proceed(new Object[]{modifiedData});
        }
        // 兜底：未知模式，不干扰
        return pjp.proceed();
    }

    /**
     * 演示模式前置处理器
     */
    private MeterDataBo preShowsProcessor(ProceedingJoinPoint pjp) {
        MeterDataBo meterDataBo = getMeterDataBo(pjp);
        meterDataBo.setFlow(keep3(0.15 + ThreadLocalRandom.current().nextDouble(0.07)));
        return meterDataBo;
    }

    /**
     * 爆管事件前置处理器
     *
     * @param point 连接点
     * @return dataBo 修改后的数据
     */
    private MeterDataBo preburstPipeProcessor(ProceedingJoinPoint point) {
        MeterDataBo dataBo = getMeterDataBo(point);

        double dp = keep3(ThreadLocalRandom.current().nextDouble(0.25, 0.35));
        double df = keep3(ThreadLocalRandom.current().nextDouble(0.25, 0.35));
        Double originalPressure = dataBo.getPressure();

        double modifiedPressure = Math.max(originalPressure - dp, 0.12);
        dataBo.setPressure(modifiedPressure);
        double modifiedFlow = Math.min(keep3(df), 0.5);

        dataBo.setFlow(modifiedFlow);
        dataBo.setStatus("burstPipe");
        return dataBo;
    }

    /**
     * 漏水事件前置处理器
     */
    private MeterDataBo preLeakingProcessor(ProceedingJoinPoint point) {
        MeterDataBo dataBo = getMeterDataBo(point);
        //重新设置流速
        double v = ThreadLocalRandom.current().nextDouble(0.05);
        double flow = 0.1 + v;

        dataBo.setFlow(flow);
        dataBo.setPressure(waterPressureGenerate(flow, serverConfig));
        return dataBo;
    }

    /**
     * 得到原方法入参
     */
    private static MeterDataBo getMeterDataBo(ProceedingJoinPoint point) {
        Object[] args = point.getArgs();
        return (MeterDataBo) args[0];
    }
}
