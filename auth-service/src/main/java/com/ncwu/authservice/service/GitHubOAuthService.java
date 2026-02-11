package com.ncwu.authservice.service;

import com.ncwu.authservice.config.oauthconfig.GitHubOAuthProperties;
import com.ncwu.authservice.exception.GitHubOAuthException;
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
import java.util.List;
import java.util.Map;

/**
 * GitHub OAuth服务
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/11
 */
@Data
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubOAuthService {

    private final WebClient.Builder webClientBuilder;
    private final GitHubOAuthProperties gitHubOAuthProperties;

    /**
     * 获取GitHub授权 URL
     */
    public String getAuthorizationUrl() {
        return String.format("%s?client_id=%s&redirect_uri=%s&scope=user:email",
                gitHubOAuthProperties.getAuthorizeUrl(),
                gitHubOAuthProperties.getClientId(),
                gitHubOAuthProperties.getRedirectUri());
    }

    /**
     * 通过授权码获取访问令牌
     */
    public String getAccessToken(String code) {
        WebClient webClient = webClientBuilder.build();

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", gitHubOAuthProperties.getClientId());
        requestBody.add("client_secret", gitHubOAuthProperties.getClientSecret());
        requestBody.add("code", code);
        requestBody.add("redirect_uri", gitHubOAuthProperties.getRedirectUri());

        try {
            Map<String, Object> response = webClient.post()
                    .uri(gitHubOAuthProperties.getAccessTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(requestBody)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))  // 重试3次，间隔2秒
                            .maxBackoff(Duration.ofSeconds(10))        // 最大间隔10秒
                            .doBeforeRetry(retrySignal -> 
                                log.warn("GitHub API请求失败，正在进行第{}次重试", 
                                    retrySignal.totalRetries() + 1)))
                    .block();

            if (response != null && response.containsKey("access_token")) {
                return (String) response.get("access_token");
            } else {
                String error = response != null ? (String) response.get("error") : "Unknown error";
                throw new GitHubOAuthException("Failed to get access token: " + error);
            }
        } catch (Exception e) {
            log.error("Failed to get GitHub access token", e);
            throw new GitHubOAuthException("Failed to get access token: " + e.getMessage());
        }
    }

    /**
     * 获取GitHub用户信息
     */
    public GitHubUserInfo getUserInfo(String accessToken) {
        WebClient webClient = webClientBuilder.build();
        try {
            // 获取用户基本信息
            Map<String, Object> userInfo = webClient.get()
                    .uri(gitHubOAuthProperties.getUserInfoUrl())
                    .header("Authorization", "token " + accessToken)
                    .header("User-Agent", "Campus-Water-IQ")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();

            // 获取用户邮箱信息
            List<Map<String, Object>> emailInfo = webClient.get()
                    .uri(gitHubOAuthProperties.getUserEmailUrl())
                    .header("Authorization", "token " + accessToken)
                    .header("User-Agent", "Campus-Water-IQ")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    })
                    .block();

            if (userInfo != null) {
                GitHubUserInfo result = new GitHubUserInfo();
                result.setId(((Number) userInfo.get("id")).longValue());
                result.setLogin((String) userInfo.get("login"));
                result.setName((String) userInfo.get("name"));
                result.setAvatarUrl((String) userInfo.get("avatar_url"));
                result.setBio((String) userInfo.get("bio"));
                result.setLocation((String) userInfo.get("location"));
                result.setCompany((String) userInfo.get("company"));

                // 获取主邮箱
                if (emailInfo != null && !emailInfo.isEmpty()) {
                    emailInfo.stream()
                            .filter(email ->
                                    Boolean.TRUE.equals(email.get("primary")))
                            .findFirst()
                            .ifPresent(email ->
                                    result.setEmail((String) email.get("email")));
                }
                return result;
            } else {
                throw new GitHubOAuthException("Failed to get user info");
            }
        } catch (Exception e) {
            log.error("Failed to get GitHub user info", e);
            throw new GitHubOAuthException("Failed to get user info: " + e.getMessage());
        }
    }

    /**
     * GitHub用户信息
     */
    @Data
    public static class GitHubUserInfo {
        // Getters and Setters
        private Long id;
        private String login;
        private String name;
        private String email;
        private String avatarUrl;
        private String bio;
        private String location;
        private String company;

    }
}
