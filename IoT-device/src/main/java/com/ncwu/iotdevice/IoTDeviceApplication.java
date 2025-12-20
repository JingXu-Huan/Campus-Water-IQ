package com.ncwu.iotdevice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class IoTDeviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IoTDeviceApplication.class, args);
    }

}
