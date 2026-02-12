package com.ncwu.watergateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS配置
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/12
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // 允许的源
        corsConfig.setAllowedOriginPatterns(List.of("*"));
        
        // 允许的HTTP方法
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 允许的请求头
        corsConfig.setAllowedHeaders(List.of("*"));
        
        // 是否允许发送Cookie
        corsConfig.setAllowCredentials(true);
        
        // 预检请求的缓存时间
        corsConfig.setMaxAge(3600L);
        
        // 暴露的响应头
        corsConfig.setExposedHeaders(Arrays.asList("X-Total-Count", "X-User-Id"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
