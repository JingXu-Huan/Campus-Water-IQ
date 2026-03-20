package com.ncwu.iotdevice.config;

import com.ncwu.iotdevice.interceptor.RoleInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 注册权限拦截器
 *
 * @author jingxu
 * @since 2026/3/20
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RoleInterceptor roleInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 公开接口，不做权限校验
                        "/device/isInit",
                        "/device/getDevicesNum",
                        "/device/getMode",
                        "/device/getSeason",
                        "/device/buildingConfig",
                        "/actuator/**"
                );
    }
}
