package com.ncwu.authservice.config;

import com.ncwu.authservice.interceptor.IpInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/19
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    
    private final IpInterceptor ipInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ipInterceptor)
                .addPathPatterns("/**")  // 拦截所有路径
                .excludePathPatterns(
                        "/actuator/**",    // 排除健康检查
                        "/signup",
                        "/signup/send-code",
                        "/user/**",        // 排除用户相关接口
                        "/error"           // 排除错误页面
                );
    }
}
