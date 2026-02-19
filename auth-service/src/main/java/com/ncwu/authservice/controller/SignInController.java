package com.ncwu.authservice.controller;


import com.ncwu.authservice.domain.VO.AuthResult;
import com.ncwu.authservice.domain.DTO.SignInRequest;
import com.ncwu.authservice.service.UserService;
import com.ncwu.authservice.service.CodeSender;
import com.ncwu.authservice.service.GitHubOAuthService;
import com.ncwu.authservice.service.WeChatOAuthService;
import com.ncwu.authservice.config.oauthconfig.GitHubOAuthProperties;
import com.ncwu.authservice.config.oauthconfig.WeChatOAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户登陆控制器
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/7
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class SignInController {

    private final UserService userService;
    private final CodeSender codeSender;
    private final GitHubOAuthService gitHubOAuthService;
    private final GitHubOAuthProperties gitHubOAuthProperties;
    private final WeChatOAuthService weChatOAuthService;
    private final WeChatOAuthProperties weChatOAuthProperties;

    /**
     * 用户登陆接口
     */
    @PostMapping("/signin")
    public AuthResult signIn(@RequestBody SignInRequest request) {
        return userService.signIn(request);
    }

    /**
     * 用户请求邮箱验证码
     */
    @PostMapping("/send-code")
    public void sendCode(@RequestBody Map<String, String> request) {
        String toEmail = request.get("toEmail");
        codeSender.sendMailCode(toEmail);
    }

    /**
     * 用户请求手机验证码
     */
    @PostMapping("/send-phone-code")
    public void sendPhoneCode(@RequestBody Map<String, String> request) {
        String phoneNum = request.get("phoneNum");
        codeSender.sendPhoneCode(phoneNum);
    }

    /**
     * 用户获取GitHub授权URL
     */
    @GetMapping("/github/authorize")
    public String getGitHubAuthorizeUrl() {
        return gitHubOAuthService.getAuthorizationUrl();
    }

    /**
     * GitHub OAuth回调处理
     */
    @GetMapping("/github/callback")
    public String githubCallback(@RequestParam String code) {
        try {
            // 这里可以处理OAuth回调，例如重定向到前端页面并传递code
            log.info("收到GitHub OAuth回调，code: {}", code);
            // todo 暂时返回成功信息，实际应用中应该重定向到前端页面
            return "GitHub OAuth授权成功，请使用返回的code进行登录: " + code;
        } catch (Exception e) {
            log.error("GitHub OAuth回调处理失败", e);
            return "GitHub OAuth授权失败: " + e.getMessage();
        }
    }

    /**
     * 用户获取WeChat授权URL
     */
    @GetMapping("/wechat/authorize")
    public String getWeChatAuthorizeUrl() {
        return weChatOAuthService.getAuthorizationUrl();
    }

    /**
     * WeChat OAuth回调处理
     */
    @GetMapping("/wechat/callback")
    public String wechatCallback(@RequestParam String code) {
        try {
            // 这里可以处理OAuth回调，例如重定向到前端页面并传递code
            log.info("收到WeChat OAuth回调，code: {}", code);
            // todo 暂时返回成功信息，实际应用中应该重定向到前端页面
            return "WeChat OAuth授权成功，请使用返回的code进行登录: " + code;
        } catch (Exception e) {
            log.error("WeChat OAuth回调处理失败", e);
            return "WeChat OAuth授权失败: " + e.getMessage();
        }
    }
}
