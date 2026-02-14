package com.ncwu.watergateway.filter;

import com.ncwu.watergateway.config.GatewaySecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 认证过滤器
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final GatewaySecurityProperties securityProperties;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int ORDER = -100;
    private SecretKey cachedKey;
    private JwtParser jwtParser;

    // 在初始化方法中执行（例如 @PostConstruct）
    @PostConstruct
    public void init() {
        String jwtSecret = securityProperties.getJwtSecret();
        this.cachedKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parserBuilder()
                .setSigningKey(this.cachedKey)
                .build();
    }
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 检查是否为不需要认证的路径
        if (isPermitAllPath(path)) {
            return chain.filter(exchange);
        }

        // 获取Authorization头
        String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            return handleUnauthorized(exchange, "Missing or invalid Authorization header");
        }

        // 提取token
        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!StringUtils.hasText(token)) {
            return handleUnauthorized(exchange, "Token is empty");
        }

        try {
            // 验证token
            Claims claims = validateToken(token);
            if (claims == null) {
                return handleUnauthorized(exchange, "Invalid token");
            }

            // 将用户信息添加到请求头中
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Name", claims.get("username", String.class))
                    .header("X-User-Role", claims.get("role", String.class))
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return handleUnauthorized(exchange, "Token validation failed");
        }
    }

    /**
     * 检查是否为允许访问的路径
     */
    private boolean isPermitAllPath(String path) {
        List<String> permitAllPaths = securityProperties.getPermitAllPaths();
        if (permitAllPaths == null) {
            return false;
        }

        return permitAllPaths.stream().anyMatch(permitPath -> {
            if (permitPath.endsWith("/**")) {
                String prefix = permitPath.substring(0, permitPath.length() - 3);
                return path.startsWith(prefix);
            }
            return path.equals(permitPath);
        });
    }

    /**
     * 验证JWT token
     */
    private Claims validateToken(String token) {
        try {
            // 直接使用预热好的解析器，不再创建对象，不再计算Key
            return jwtParser.parseClaimsJws(token).getBody();
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 处理未授权请求
     */
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = String.format("{\"code\":401,\"message\":\"%s\",\"timestamp\":%d}", 
                message, System.currentTimeMillis());
        
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
