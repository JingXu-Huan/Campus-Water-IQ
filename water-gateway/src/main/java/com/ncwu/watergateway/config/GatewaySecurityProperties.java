package com.ncwu.watergateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 网关安全配置属性
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/12
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {

    /**
     * 不需要认证的路径
     */
    private List<String> permitAllPaths;

    /**
     * JWT密钥
     */
    private String jwtSecret;

    /**
     * 限流配置
     */
    private RateLimit rateLimit = new RateLimit();

    /**
     * 限流配置
     */
    @Data
    public static class RateLimit {
        /**
         * 是否启用限流
         */
        private boolean enabled = true;

        /**
         * 每秒请求数
         */
        private int replenishRate = 10;

        /**
         * 突发容量
         */
        private int burstCapacity = 20;
    }
}
