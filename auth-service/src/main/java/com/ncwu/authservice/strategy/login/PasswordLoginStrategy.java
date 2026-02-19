package com.ncwu.authservice.strategy.login;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncwu.authservice.domain.VO.AuthResult;
import com.ncwu.authservice.domain.BO.UserInfo;
import com.ncwu.authservice.domain.enums.LoginType;
import com.ncwu.authservice.domain.DTO.SignInRequest;
import com.ncwu.authservice.domain.entity.User;
import com.ncwu.authservice.exception.DeserializationFailedException;
import com.ncwu.authservice.factory.login.LoginStrategy;
import com.ncwu.authservice.mapper.UserMapper;
import com.ncwu.authservice.service.TokenHelper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

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
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

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
        //查询缓存
        String jsonInfo = redisTemplate.opsForValue().get("UserInfo:" + password);
        UserInfo userInfo;
        if (jsonInfo != null) {
            try {
                userInfo = objectMapper.readValue(jsonInfo, UserInfo.class);
            } catch (JsonProcessingException e) {
                throw new DeserializationFailedException("反序列化失败");
            }
            if (userInfo != null) {
                return getAuthResult(userInfo.getNickName(), userInfo.getPassword(),
                        userInfo.getUserType(), userInfo.getStatus(), password, uid);
            }
        } else {
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUid, uid)
                    .select(User::getPassword, User::getStatus, User::getUserType, User::getNickName));
            if (user == null) {
                return new AuthResult(false);
            } else {
                try {
                    redisTemplate.opsForValue().set("UserInfo:" + password, objectMapper.writeValueAsString(user)
                            , 300 + ThreadLocalRandom.current().nextInt(60), TimeUnit.SECONDS);
                } catch (JsonProcessingException e) {
                    throw new DeserializationFailedException("序列化异常");
                }
            }
            String nickName = user.getNickName();
            Integer userType = user.getUserType();
            Integer status = user.getStatus();
            String validPwd = user.getPassword();
            return getAuthResult(nickName, validPwd, userType, status, password, uid);
        }
        return new AuthResult(false);
    }

    private @NonNull AuthResult getAuthResult(String nickName, String validPwd,
                                              Integer userType, Integer status, String password, String uid) {
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
