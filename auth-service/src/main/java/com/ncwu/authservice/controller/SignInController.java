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
import org.springframework.web.servlet.view.RedirectView;

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
    public RedirectView githubCallback(@RequestParam String code) {
        try {
            log.info("收到GitHub OAuth回调，code: {}", code);
            // 重定向到前端页面，并传递授权码
            String redirectUrl = "http://localhost:5173/login?github_code=" + code;
            return new RedirectView(redirectUrl);
        } catch (Exception e) {
            log.error("GitHub OAuth回调处理失败", e);
            // 重定向到前端登录页面，并传递错误信息
            String redirectUrl = "http://localhost:5173/login?error=github_oauth_failed";
            return new RedirectView(redirectUrl);
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
    public RedirectView wechatCallback(@RequestParam String code) {
        try {
            log.info("收到WeChat OAuth回调，code: {}", code);
            // 重定向到前端页面，并传递授权码
            String redirectUrl = "http://localhost:5173/login?wechat_code=" + code;
            return new RedirectView(redirectUrl);
        } catch (Exception e) {
            log.error("WeChat OAuth回调处理失败", e);
            // 重定向到前端登录页面，并传递错误信息
            String redirectUrl = "http://localhost:5173/login?error=wechat_oauth_failed";
            return new RedirectView(redirectUrl);
        }
    }
}
