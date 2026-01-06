package com.ncwu.iotdevice.AOP.Aspects;


import com.ncwu.iotdevice.AOP.annotation.Time;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 对方法耗时进行监控
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/2
 */
@Aspect
@Component
public class GetMethodsCostTime {

    @Order(1)
    @Around("@annotation(time)")
    public Object getMethodCostedTime(ProceedingJoinPoint pjp, Time time) throws Throwable {
        // 获取方法名
        String methodName = pjp.getSignature().toShortString();
        // 记录开始时间
        long start = System.currentTimeMillis();
        // 执行目标方法
        Object result = pjp.proceed();
        // 记录结束时间
        long end = System.currentTimeMillis();
        // 计算耗时
        long cost = end - start;
        // 打印日志
        System.out.println("方法 " + methodName + " 执行耗时：" + cost + " ms");
        return result;
    }
}
