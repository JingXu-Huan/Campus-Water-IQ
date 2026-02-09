package com.ncwu.authservice.service;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncwu.authservice.entity.AuthResult;
import com.ncwu.authservice.entity.LoginType;
import com.ncwu.authservice.entity.SignInRequest;
import com.ncwu.authservice.entity.User;
import com.ncwu.authservice.factory.LoginStrategy;
import com.ncwu.authservice.factory.LoginStrategyFactory;
import com.ncwu.authservice.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/7
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final LoginStrategyFactory factory;

    @Override
    public AuthResult signIn(SignInRequest request) {
        LoginType type = request.getType();
        LoginStrategy strategy = factory.get(type);
        return strategy.login(request);
    }
}
