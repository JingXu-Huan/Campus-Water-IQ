package com.ncwu.authservice.config.oauthconfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * GitHub OAuth配置属性
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/11
 */
@Data
@Component
//将application.yml里作为"github.oauth"前缀的属性绑定到成员变量
@ConfigurationProperties(prefix = "github.oauth")
public class GitHubOAuthProperties {

    /**
     * GitHub OAuth应用ID
     */
    private String clientId;

    /**
     * GitHub OAuth应用密钥
     */
    private String clientSecret;

    /**
     * GitHub OAuth重定向URI
     */
    private String redirectUri;

    /**
     * GitHub OAuth授权URL
     */
    private String authorizeUrl = "https://github.com/login/oauth/authorize";

    /**
     * GitHub OAuth访问令牌URL
     */
    private String accessTokenUrl = "https://github.com/login/oauth/access_token";

    /**
     * GitHub用户信息API URL
     */
    private String userInfoUrl = "https://api.github.com/user";

    /**
     * GitHub用户邮箱API URL
     */
    private String userEmailUrl = "https://api.github.com/user/emails";
}
