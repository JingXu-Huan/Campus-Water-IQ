package com.ncwu.authservice.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncwu.authservice.domain.DTO.SignUpRequest;
import com.ncwu.authservice.domain.VO.AuthResult;
import com.ncwu.authservice.domain.VO.SignUpResult;
import com.ncwu.authservice.domain.enums.LoginType;
import com.ncwu.authservice.domain.DTO.SignInRequest;
import com.ncwu.authservice.domain.entity.User;
import com.ncwu.authservice.domain.enums.SignUpType;
import com.ncwu.authservice.factory.login.LoginStrategy;
import com.ncwu.authservice.factory.login.LoginStrategyFactory;
import com.ncwu.authservice.factory.signup.SignUpStrategy;
import com.ncwu.authservice.factory.signup.SignUpStrategyFactory;
import com.ncwu.authservice.mapper.UserMapper;
import com.ncwu.authservice.service.UserService;
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

    private final LoginStrategyFactory loginStrategyFactory;
    private final SignUpStrategyFactory signUpStrategyFactory;

    @Override
    public AuthResult signIn(SignInRequest request) {
        LoginType type = request.getType();
        LoginStrategy strategy = loginStrategyFactory.get(type);
        return strategy.login(request);
    }

    @Override
    public SignUpResult signUp(SignUpRequest request) {
        SignUpType type = request.getType();
        SignUpStrategy strategy = signUpStrategyFactory.get(type);
        return strategy.signUp(request);
    }
}
