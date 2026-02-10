package com.ncwu.authservice.controller;


import com.ncwu.authservice.entity.AuthResult;
import com.ncwu.authservice.entity.SignInRequest;
import com.ncwu.authservice.service.UserService;
import com.ncwu.authservice.service.CodeSender;
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

    /**
     * 用户登陆接口
     */
    @PostMapping("/signin")
    public AuthResult signIn(@RequestBody SignInRequest request) {
        return userService.signIn(request);
    }

    /**
     * 用户请求验证码
     */
    @PostMapping("/send-code")
    public void sendCode(@RequestBody Map<String, String> request) {
        String toEmail = request.get("toEmail");
        codeSender.sendCode(toEmail);
    }
}
