package com.ncwu.authservice.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ncwu.authservice.entity.AuthResult;
import com.ncwu.authservice.entity.SignInRequest;
import com.ncwu.authservice.entity.User;


/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/7
 */
public interface UserService extends IService<User> {
    AuthResult signIn(SignInRequest request);
}
