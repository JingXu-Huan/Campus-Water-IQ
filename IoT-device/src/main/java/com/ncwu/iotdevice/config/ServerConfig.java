package com.ncwu.iotdevice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "server")
@Data
public class ServerConfig {
    private String reportFrequency;
    private String timeOffset;
    //管网的初始压力
    private double p0;
    //数据离散步长
    private double step;
    //管网的最小压力
    double Pmin;
    //管网的最大压力
    double Pmax;
}