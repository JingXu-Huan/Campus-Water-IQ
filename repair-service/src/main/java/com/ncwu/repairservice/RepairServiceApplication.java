package com.ncwu.repairservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.ncwu.repairservice.mapper")
public class RepairServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RepairServiceApplication.class, args);
    }
}
