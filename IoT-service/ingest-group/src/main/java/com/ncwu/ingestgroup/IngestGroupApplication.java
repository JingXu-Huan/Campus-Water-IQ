package com.ncwu.ingestgroup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class IngestGroupApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngestGroupApplication.class, args);
    }

}
