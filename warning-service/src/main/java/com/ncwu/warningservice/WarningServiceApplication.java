package com.ncwu.warningservice;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo
public class WarningServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WarningServiceApplication.class, args);
    }

}
