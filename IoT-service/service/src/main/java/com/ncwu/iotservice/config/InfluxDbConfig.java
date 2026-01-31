package com.ncwu.iotservice.config;


import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/18
 */
@Configuration
public class InfluxDbConfig {

    @Bean
    public InfluxDBClient influxDBClient(){
        char[] influxTokens = System.getenv("INFLUX_TOKEN").toCharArray();
        return InfluxDBClientFactory.create("http://localhost:8086",influxTokens,"jingxu","water");
    }
}
