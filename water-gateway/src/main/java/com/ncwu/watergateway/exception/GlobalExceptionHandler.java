package com.ncwu.watergateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关全局异常处理器
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/12
 */
@Slf4j
@Component
@Order(-1)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // 设置响应头
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        // 根据异常类型设置状态码和错误信息
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Internal Server Error";
        int code = 500;

        switch (ex) {
            case ResponseStatusException rse -> {
                status = HttpStatus.valueOf(rse.getStatusCode().value());
                message = rse.getReason();
                code = status.value();
            }
            case java.net.ConnectException connectException -> {
                status = HttpStatus.SERVICE_UNAVAILABLE;
                message = "Service unavailable";
                code = 503;
            }
            case java.util.concurrent.TimeoutException timeoutException -> {
                status = HttpStatus.GATEWAY_TIMEOUT;
                message = "Gateway timeout";
                code = 504;
            }
            default -> {
            }
        }

        response.setStatusCode(status);
        
        // 构建错误响应体
        String body = String.format(
                "{\"code\":%d,\"message\":\"%s\",\"timestamp\":%d,\"path\":\"%s\"}",
                code, message, System.currentTimeMillis(), exchange.getRequest().getURI().getPath()
        );

        log.error("Gateway error: {} - {}", status.value(), message, ex);

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
