package com.ncwu.authservice.service;

import com.ncwu.authservice.config.oauthconfig.WeChatOAuthProperties;
import com.ncwu.authservice.exception.WeChatOAuthException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

/**
 * WeChat OAuth服务
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/12
 */
@Data
@Slf4j
@Service
@RequiredArgsConstructor
public class WeChatOAuthService {

    private final WebClient.Builder webClientBuilder;
    private final WeChatOAuthProperties weChatOAuthProperties;

    /**
     * 获取WeChat授权 URL
     */
    public String getAuthorizationUrl() {
        return String.format("%s?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_login&state=STATE",
                weChatOAuthProperties.getAuthorizeUrl(),
                weChatOAuthProperties.getAppId(),
                weChatOAuthProperties.getRedirectUri());
    }

    /**
     * 通过授权码获取访问令牌
     */
    public String getAccessToken(String code) {
        WebClient webClient = webClientBuilder.build();

        String url = String.format("%s?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
                weChatOAuthProperties.getAccessTokenUrl(),
                weChatOAuthProperties.getAppId(),
                weChatOAuthProperties.getAppSecret(),
                code);

        try {
            Map<String, Object> response = webClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                            .maxBackoff(Duration.ofSeconds(10))
                            .doBeforeRetry(retrySignal -> 
                                log.warn("WeChat API请求失败，正在进行第{}次重试", 
                                    retrySignal.totalRetries() + 1)))
                    .block();

            if (response != null && response.containsKey("access_token")) {
                return (String) response.get("access_token");
            } else {
                String error = response != null ? (String) response.get("errmsg") : "Unknown error";
                throw new WeChatOAuthException("Failed to get access token: " + error);
            }
        } catch (Exception e) {
            log.error("Failed to get WeChat access token", e);
            throw new WeChatOAuthException("Failed to get access token: " + e.getMessage());
        }
    }

    /**
     * 获取WeChat用户信息
     */
    public WeChatUserInfo getUserInfo(String accessToken, String openId) {
        WebClient webClient = webClientBuilder.build();
        
        String url = String.format("%s?access_token=%s&openid=%s&lang=zh_CN",
                weChatOAuthProperties.getUserInfoUrl(),
                accessToken,
                openId);

        try {
            Map<String, Object> userInfo = webClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();

            if (userInfo != null && !userInfo.containsKey("errcode")) {
                WeChatUserInfo result = new WeChatUserInfo();
                result.setOpenId((String) userInfo.get("openid"));
                result.setNickname((String) userInfo.get("nickname"));
                result.setSex((Integer) userInfo.get("sex"));
                result.setProvince((String) userInfo.get("province"));
                result.setCity((String) userInfo.get("city"));
                result.setCountry((String) userInfo.get("country"));
                result.setHeadImgUrl((String) userInfo.get("headimgurl"));
                result.setUnionId((String) userInfo.get("unionid"));
                return result;
            } else {
                String error = userInfo != null ? (String) userInfo.get("errmsg") : "Unknown error";
                throw new WeChatOAuthException("Failed to get user info: " + error);
            }
        } catch (Exception e) {
            log.error("Failed to get WeChat user info", e);
            throw new WeChatOAuthException("Failed to get user info: " + e.getMessage());
        }
    }

    /**
     * 通过授权码获取用户信息（组合操作）
     */
    public WeChatUserInfo getUserInfoByCode(String code) {
        String accessToken = getAccessToken(code);
        // 从access_token响应中获取openid
        WebClient webClient = webClientBuilder.build();
        
        String url = String.format("%s?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
                weChatOAuthProperties.getAccessTokenUrl(),
                weChatOAuthProperties.getAppId(),
                weChatOAuthProperties.getAppSecret(),
                code);

        try {
            Map<String, Object> tokenResponse = webClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();

            if (tokenResponse != null && tokenResponse.containsKey("openid")) {
                String openId = (String) tokenResponse.get("openid");
                return getUserInfo(accessToken, openId);
            } else {
                throw new WeChatOAuthException("Failed to get openid from token response");
            }
        } catch (Exception e) {
            log.error("Failed to get WeChat user info by code", e);
            throw new WeChatOAuthException("Failed to get user info by code: " + e.getMessage());
        }
    }

    /**
     * WeChat用户信息
     */
    @Data
    public static class WeChatUserInfo {
        private String openId;
        private String nickname;
        private Integer sex;
        private String province;
        private String city;
        private String country;
        private String headImgUrl;
        private String unionId;
    }
}
