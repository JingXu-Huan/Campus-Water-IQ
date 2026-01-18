package com.ncwu.iotdevice;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@EnableDubbo
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true) // 必须设置为 true
public class IoTDeviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IoTDeviceApplication.class, args);
    }

}
