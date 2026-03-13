package com.ncwu.iotservice.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/13
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "service")
public class ServiceConfig {
    //导出报表的报表最大长度
    public int maxSize;
    //保存数据时间间隔
    public int saveTimeInterval;
}
