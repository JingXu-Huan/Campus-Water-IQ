package com.ncwu.iotdevice.AOP;


import com.ncwu.iotdevice.AOP.annotation.CloseValue;
import com.ncwu.iotdevice.domain.Bo.MeterDataBo;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/4
 */
@Order(4)
@Aspect
@Component
@RequiredArgsConstructor
public class ClosedTheValue {

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(closeValue)")
    public Object makeFlowToZero(ProceedingJoinPoint proceedingJoinPoint, CloseValue closeValue) throws Throwable {
        MeterDataBo dataBo = (MeterDataBo) proceedingJoinPoint.getArgs()[0];
        Boolean isClosed = redisTemplate.opsForSet().isMember("meter_closed", dataBo.getDeviceId());
        if (Boolean.TRUE.equals(isClosed)){
            dataBo.setFlow(0.0);
        }
        return proceedingJoinPoint.proceed(new Object[]{dataBo});
    }
}
