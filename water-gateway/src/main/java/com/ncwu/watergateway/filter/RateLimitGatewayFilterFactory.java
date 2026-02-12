package com.ncwu.watergateway.filter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * 限流过滤器
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/12
 */
@Slf4j
@Component
public class RateLimitGatewayFilterFactory extends AbstractGatewayFilterFactory<RateLimitGatewayFilterFactory.Config> {

    public RateLimitGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new RateLimitGatewayFilter(config);
    }

    @Data
    public static class Config {
        private int replenishRate = 10;
        private int burstCapacity = 20;
        private boolean enabled = true;
    }

    private static class RateLimitGatewayFilter implements GatewayFilter, Ordered {
        private final Config config;

        public RateLimitGatewayFilter(Config config) {
            this.config = config;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            if (!config.isEnabled()) {
                return chain.filter(exchange);
            }

            // 这里实现简单的限流逻辑
            // 在生产环境中，建议使用Redis + Lua脚本实现分布式限流
            String clientId = getClientId(exchange.getRequest());
            
            // 简单的内存限流（仅用于演示）
            if (isRateLimited(clientId)) {
                return handleRateLimitExceeded(exchange);
            }

            return chain.filter(exchange);
        }

        private String getClientId(ServerHttpRequest request) {
            // 使用IP地址作为客户端标识
            String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            String xRealIp = request.getHeaders().getFirst("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
            
            return request.getRemoteAddress() != null ? 
                    request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
        }

        private boolean isRateLimited(String clientId) {
            // 这里应该实现基于时间窗口的限流逻辑
            // 为了演示，这里返回false，即不限流
            // 实际实现可以使用Redis的令牌桶或滑动窗口算法
            return false;
        }

        private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().add("Content-Type", "application/json");
            
            String body = String.format("{\"code\":429,\"message\":\"Rate limit exceeded\",\"timestamp\":%d}", 
                    System.currentTimeMillis());
            
            return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
        }

        @Override
        public int getOrder() {
            return -50;
        }
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("replenishRate", "burstCapacity", "enabled");
    }
}
