package com.ncwu.authservice.controller;


import com.ncwu.authservice.domain.DTO.SignUpRequest;
import com.ncwu.authservice.domain.VO.SignUpResult;
import com.ncwu.authservice.service.CodeSender;
import com.ncwu.authservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户注册控制器
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/7
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/signup")
public class SignUpController {
    private final CodeSender codeSender;
    private final UserService userService;

    /**
     * 用户注册账户
     */
    @PutMapping
    public SignUpResult signUp(@RequestBody SignUpRequest signUpRequest) {
        return userService.signUp(signUpRequest);
    }

    /**
     * 用户请求邮箱验证码
     */
    @PostMapping("/send-code")
    public void sendCode(@RequestBody Map<String, String> request) {
        String toEmail = request.get("toEmail");
        codeSender.sendMailCode(toEmail);
    }
}
