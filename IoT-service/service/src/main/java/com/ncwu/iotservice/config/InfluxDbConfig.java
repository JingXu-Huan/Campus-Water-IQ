package com.ncwu.iotservice.config;


import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
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

    @Value("${influx.url:http://localhost:8086}")
    private String influxUrl;

    @Value("${influx.org:ncwu}")
    private String influxOrg;

    @Value("${influx.bucket:water}")
    private String influxBucket;

    @Bean
    public InfluxDBClient influxDBClient(){
        if (influxToken == null || influxToken.trim().isEmpty()) {
            throw new IllegalArgumentException("InfluxDB token is not configured. Please set 'influx.token' in application.yml or INFLUX_TOKEN environment variable.");
        }
        char[] influxTokens = influxToken.toCharArray();

        return InfluxDBClientFactory.create(influxUrl, influxTokens, influxOrg, influxBucket);
    }

    @Bean
    public QueryApi getQueryApi(InfluxDBClient influxDBClient){
        return influxDBClient.getQueryApi();
    }
}
