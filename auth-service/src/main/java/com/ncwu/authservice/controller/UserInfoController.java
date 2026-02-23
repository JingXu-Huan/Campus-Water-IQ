package com.ncwu.authservice.controller;


import com.ncwu.authservice.service.UserService;
import com.ncwu.common.domain.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/23
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserInfoController {

    private final UserService userService;

    /**
     * 用户上传自己的头像
     */
    @PostMapping("/avatar")
    public Result<Boolean> uploadAvatar(@RequestParam("image") MultipartFile file, String uid) {
        return userService.uploadAvatar(file, uid);
    }

    /**
     * 获取头像地址
     */
    @GetMapping("/getAvatat")
    public Result<String> getAvatar(String uid) {
        return userService.getAvatar(uid);
    }

}
