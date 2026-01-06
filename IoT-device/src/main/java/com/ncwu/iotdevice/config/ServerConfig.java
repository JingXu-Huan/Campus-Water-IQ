package com.ncwu.iotdevice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "server")
@Data
public class ServerConfig {
    //水质传感器上报频率，以秒为单位
    private String waterQualityReportFrequency;
    //水质传感器上报偏移时间，以秒为单位
    private String waterQualityReportTimeOffset;
    //水表上报频率，以秒为单位
    private String meterReportFrequency;
    //水表上报偏移时间，以秒为单位
    private String meterTimeOffset;
    //管网的初始压力
    private double p0;
    //管网数据离散步长
    private double step;
    //管网的最小压力
    double Pmin;
    //管网的最大压力
    double Pmax;
    //数据不可信发生概率
    double PnotCredible;
    //错过 n 个上报周期视为下线
    int n;
}