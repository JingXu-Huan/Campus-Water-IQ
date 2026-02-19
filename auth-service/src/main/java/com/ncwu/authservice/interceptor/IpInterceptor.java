package com.ncwu.authservice.interceptor;

import com.ncwu.authservice.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/17
 */

@Slf4j
@Component
public class IpInterceptor implements HandlerInterceptor {
    private final RateLimiterService rateLimiterService;

    public IpInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        String ip = getClientIp(request);
        log.info("IP拦截器触发，IP: {}", ip);
        // 限流判断
        if (!rateLimiterService.tryAcquire(ip)) {
            response.setStatus(429);
            log.warn("IP被限流: {}", ip);
            return false;
        }
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}
