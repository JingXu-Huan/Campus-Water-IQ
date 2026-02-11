package com.ncwu.authservice.config.proxy;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 代理配置属性
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/11
 */
@Data
@Component
@ConfigurationProperties(prefix = "proxy")
public class ProxyConfig {
    
    /**
     * 是否启用代理
     */
    private boolean enabled = false;
    
    /**
     * 代理主机
     */
    private String host = "127.0.0.1";
    
    /**
     * 代理端口
     */
    private int port = 7897;
    
    /**
     * 代理用户名
     */
    private String username;
    
    /**
     * 代理密码
     */
    private String password;
}
