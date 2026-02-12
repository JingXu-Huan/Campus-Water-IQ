package com.ncwu.iotdevice.config.MonitorConfig;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置消息队列监控指标
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/12
 */
@Configuration
public class CounterConfig {
    @Bean
    public Counter messageSuccessCounter(MeterRegistry registry){
        return Counter.builder("mq.send.success")
                .description("消息发送成功次数")
                .tag("queue", "data-queue")
                .register(registry);
    }
    @Bean
    public Counter messageFailureCounter(MeterRegistry registry){
        return Counter.builder("mq.send.failure")
                .description("消息发送失败次数")
                .tag("queue", "data-queue")
                .register(registry);
    }
}
