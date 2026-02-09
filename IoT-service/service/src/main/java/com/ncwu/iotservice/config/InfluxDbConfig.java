package com.ncwu.iotservice.config;


import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/18
 */
@Configuration
public class InfluxDbConfig {

    @Value("${influx.token:}")
    private String influxToken;

    @Bean
    public InfluxDBClient influxDBClient(){
        if (influxToken == null || influxToken.trim().isEmpty()) {
            throw new IllegalArgumentException("InfluxDB token is not configured. Please set 'influx.token' in application.yml or INFLUX_TOKEN environment variable.");
        }
        char[] influxTokens = influxToken.toCharArray();
        return InfluxDBClientFactory.create("http://localhost:8086",influxTokens,"ncwu","water");
    }
}
