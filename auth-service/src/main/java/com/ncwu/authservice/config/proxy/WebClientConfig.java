package com.ncwu.authservice.config.proxy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;
import io.netty.channel.ChannelOption;
import java.time.Duration;

/**
 * WebClient配置
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/11
 */
@Configuration
public class WebClientConfig {

    @Value("${http.proxy.host:}")
    private String proxyHost;

    @Value("${http.proxy.port:0}")
    private int proxyPort;
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        // 配置 HttpClient 超时时间和代理
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60))  // 响应超时 60 秒
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000);  // 连接超时 30 秒

        // 容器中的 127.0.0.1 只指向容器自身，代理必须显式通过环境变量配置。
        if (StringUtils.hasText(proxyHost) && proxyPort > 0) {
            httpClient = httpClient.proxy(proxy -> proxy
                        .type(ProxyProvider.Proxy.HTTP)
                        .host(proxyHost)
                        .port(proxyPort)
                        .connectTimeoutMillis(30000));
        }
        
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer ->
                        configurer.defaultCodecs().maxInMemorySize(1024 * 1024)); // 1MB
    }
}
