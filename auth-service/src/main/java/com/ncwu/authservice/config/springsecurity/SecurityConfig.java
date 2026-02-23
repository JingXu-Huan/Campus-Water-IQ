package com.ncwu.authservice.config.springsecurity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置类
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/10
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF，因为我们使用 JWT
                .csrf(AbstractHttpConfigurer::disable)
                // 设置会话管理为无状态
                .sessionManagement(session -> 
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置授权规则
                .authorizeHttpRequests(auth -> auth
                        // 放行登录相关端点
                        .requestMatchers("/signup/**").permitAll()
                        .requestMatchers("/auth/signin").permitAll()
                        .requestMatchers("/auth/send-code").permitAll()
                        .requestMatchers("/auth/send-phone-code").permitAll()
                        // 放行 GitHub OAuth 相关端点
                        .requestMatchers("/auth/github/authorize").permitAll()
                        .requestMatchers("/auth/github/callback").permitAll()
                        // 放行 WeChat OAuth 相关端点
                        .requestMatchers("/auth/wechat/authorize").permitAll()
                        .requestMatchers("/auth/wechat/callback").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 放行头像相关端点
                        .requestMatchers("/user/avatar").permitAll()
                        .requestMatchers("/user/getAvatat").permitAll()
                        // 放行健康检查端点
                        .requestMatchers("/actuator/**").permitAll()
                        // 其他所有请求都需要认证（包括 /user/**）
                        .anyRequest().authenticated()
                );
        
        return http.build();
    }
    
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
