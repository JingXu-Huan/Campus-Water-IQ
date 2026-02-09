package com.ncwu.authservice.strategy;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ncwu.authservice.entity.AuthResult;
import com.ncwu.authservice.entity.LoginType;
import com.ncwu.authservice.entity.SignInRequest;
import com.ncwu.authservice.entity.User;
import com.ncwu.authservice.factory.LoginStrategy;
import com.ncwu.authservice.mapper.UserMapper;
import com.ncwu.authservice.service.TokenHelper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/7
 */
@Component
@RequiredArgsConstructor
public class PasswordLoginStrategy implements LoginStrategy {

    private final List<RBloomFilter<String>> bloomFilters;
    private RBloomFilter<String> passwordBloomFilter;
    private final UserMapper userMapper;
    private final TokenHelper tokenHelper;

    @PostConstruct
    void init() {
        passwordBloomFilter = bloomFilters.getLast();
    }

    @Override
    public LoginType getType() {
        return LoginType.PASSWORD;
    }

    @Override
    public AuthResult login(SignInRequest request) {
        String uid = request.getIdentifier();
        String password = request.getCredential();
        if (!passwordBloomFilter.contains(uid)) {
            return new AuthResult(false);
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUid, uid)
                .select(User::getPassword, User::getStatus, User::getUserType, User::getNickName));
        if (user == null) {
            return new AuthResult(false);
        }
        String nickName = user.getNickName();
        Integer userType = user.getUserType();
        Integer status = user.getStatus();
        String validPwd = user.getPassword();
        if (status != 1) {
            //账号不正常
            return new AuthResult(false);
        }
        if (!password.equals(validPwd)) {
            return new AuthResult(false);
        }
        String token = tokenHelper.genToken(uid, nickName, userType);
        return new AuthResult(true, uid, token);
    }
}
