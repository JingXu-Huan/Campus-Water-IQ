package com.ncwu.authservice.controller;


import com.ncwu.authservice.service.UserService;
import com.ncwu.common.annotation.RequireRole;
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
    public Result<Boolean> uploadAvatar(@RequestParam("image") MultipartFile file, @RequestParam String uid) {
        return userService.uploadAvatar(file, uid);
    }

    /**
     * 获取头像地址
     */
    @GetMapping("/getAvatat")
    public Result<String> getAvatar(String uid) {
        return userService.getAvatar(uid);
    }

    /**
     * 用户修改自己的昵称
     */
    @PostMapping("/changeNickName")
    public Result<Boolean> updateNickName(@RequestParam String newName, @RequestParam String uid) {
        return userService.changeNickName(newName, uid);
    }

    /**
     * 用户修改自己的密码
     */
    @PostMapping("/changePwd")
    public Result<Boolean> changePwd(String oldPwd, String newPwd, String uid) {
        return userService.changePwd(oldPwd, newPwd, uid);
    }

    /**
     * 用户绑定某一个 device
     */
    @PostMapping("/bindingDevice")
    public Result<Boolean> bindingDevice(String uid, String deviceCode) {
        return userService.bindingDevice(uid, deviceCode);
    }

    /**
     * 封禁某个用户
     */
    @RequireRole(value = {3}, names = {"管理员"})
    @PostMapping("/FobbidenSomeUser")
    public Result<Boolean> forbiddenSomeUser(String uid) {
        return userService.forbiddenSomeUser(uid);

    }

    /**
     * 解除封禁某个用户
     */
    @RequireRole(value = {3}, names = {"管理员"})
    @PostMapping("/unFobbidenSomeUser")
    public Result<Boolean> unForbiddenSomeUser(String uid) {
        return userService.unforbiddenSomeUser(uid);
    }

    /**
     * 更改用户类型
     */
    @RequireRole(value = {3}, names = {"管理员"})
    @PostMapping("/chengeRole")
    public Result<Boolean> changeRole(String uid, Integer newRole) {
        return userService.changeRole(uid, newRole);
    }

    /**
     * 找回密码
     */
    @PostMapping("/FoundPwd")
    public Result<Boolean> foundPwd(String code) {
        // 这里可以调用邮件服务发送重置密码的链接或验证码
        return userService.foundPwd(code);
    }
}
