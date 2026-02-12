package com.ncwu.authservice.config.oauthconfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * WeChat OAuth配置属性
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/12
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.oauth")
public class WeChatOAuthProperties {

    /**
     * WeChat OAuth应用ID
     */
    private String appId;

    /**
     * WeChat OAuth应用密钥
     */
    private String appSecret;

    /**
     * WeChat OAuth重定向URI
     */
    private String redirectUri;

    /**
     * WeChat OAuth授权URL
     */
    private String authorizeUrl = "https://open.weixin.qq.com/connect/qrconnect";

    /**
     * WeChat OAuth访问令牌URL
     */
    private String accessTokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token";

    /**
     * WeChat用户信息API URL
     */
    private String userInfoUrl = "https://api.weixin.qq.com/sns/userinfo";

    /**
     * WeChat刷新令牌URL
     */
    private String refreshTokenUrl = "https://api.weixin.qq.com/sns/oauth2/refresh_token";
}
