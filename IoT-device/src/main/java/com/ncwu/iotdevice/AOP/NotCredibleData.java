package com.ncwu.iotdevice.AOP;


import com.ncwu.iotdevice.AOP.annotation.NotCredible;
import com.ncwu.iotdevice.config.ServerConfig;
import com.ncwu.iotdevice.domain.Bo.MeterDataBo;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

import static com.ncwu.iotdevice.utils.Utils.keep3;

/**
 * 一定概率的将数据置为不可信
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/2
 */
@Aspect
@Component
@RequiredArgsConstructor
public class NotCredibleData {

    private final ServerConfig serverConfig;

    @Order(3)
    @Around("@annotation(notCredible)")
    public Object makeDataNotCredible(ProceedingJoinPoint pjp, NotCredible notCredible) throws Throwable {
        double pnotCredible = serverConfig.getPnotCredible();
        double v = Math.random();
        //事件发生
        if (v <= pnotCredible) {

            return pjp.proceed(new MeterDataBo[]{dataNotCredibleProcessor(pjp)});
        } else {
            return pjp.proceed();
        }
    }

    public MeterDataBo dataNotCredibleProcessor(ProceedingJoinPoint pjp) {
        MeterDataBo dataBo = (MeterDataBo) pjp.getArgs()[0];
        double p = ThreadLocalRandom.current().nextDouble(1);
        if (p <= 0.4) {
            dataBo.setFlow(keep3(dataBo.getFlow() * ThreadLocalRandom.current().nextDouble(3, 5)));
        } else if (p <= 0.8) {
            dataBo.setWaterTem(keep3(dataBo.getWaterTem() * ThreadLocalRandom.current().nextDouble(2, 4)));
        } else {
            dataBo.setPressure(keep3(dataBo.getPressure() * ThreadLocalRandom.current().nextDouble(2, 4)));
        }
        //模拟边缘设备计算
        dataBo.setStatus("error");
        return dataBo;
    }
}
