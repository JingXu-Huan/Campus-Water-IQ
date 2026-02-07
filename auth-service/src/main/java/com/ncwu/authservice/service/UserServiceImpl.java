package com.ncwu.authservice.service;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncwu.authservice.entity.AuthResult;
import com.ncwu.authservice.entity.SignInRequest;
import com.ncwu.authservice.entity.User;
import com.ncwu.authservice.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/7
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public AuthResult signIn(SignInRequest request) {
        return null;
    }
}
