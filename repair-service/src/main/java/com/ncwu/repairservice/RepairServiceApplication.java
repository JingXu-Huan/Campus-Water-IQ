package com.ncwu.repairservice;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableDubbo
@SpringBootApplication
@MapperScan("com.ncwu.repairservice.mapper")
public class RepairServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RepairServiceApplication.class, args);
    }
}
