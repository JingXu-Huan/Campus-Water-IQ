package com.ncwu.authservice.config.proxy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
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
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        // 配置代理（根据你的代理设置修改这些参数）
        String proxyHost = "127.0.0.1";  // 你的代理服务器地址
        int proxyPort = 7897;           // 你的代理端口
        String proxyUsername = null;    // 代理用户名（如果需要）
        String proxyPassword = null;    // 代理密码（如果需要）
        
        // 配置 HttpClient 超时时间和代理
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60))  // 响应超时 60 秒
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)  // 连接超时 30 秒
                .proxy(proxy -> proxy
                        .type(ProxyProvider.Proxy.HTTP)
                        .host(proxyHost)
                        .port(proxyPort)
                        .connectTimeoutMillis(30000)
                );
        
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer ->
                        configurer.defaultCodecs().maxInMemorySize(1024 * 1024)); // 1MB
    }
}
