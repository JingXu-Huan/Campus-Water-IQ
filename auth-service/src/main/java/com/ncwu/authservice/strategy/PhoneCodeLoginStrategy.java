package com.ncwu.authservice.strategy;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ncwu.authservice.domain.VO.AuthResult;
import com.ncwu.authservice.domain.enums.LoginType;
import com.ncwu.authservice.domain.DTO.SignInRequest;
import com.ncwu.authservice.domain.entity.User;
import com.ncwu.authservice.factory.LoginStrategy;
import com.ncwu.authservice.mapper.UserMapper;
import com.ncwu.authservice.service.TokenHelper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/7
 */
@Component
@RequiredArgsConstructor
public class PhoneCodeLoginStrategy implements LoginStrategy {

    private final List<RBloomFilter<String>> bloomFilters;
    private RBloomFilter<String> phoneBloomFilter;
    private final StringRedisTemplate redisTemplate;
    private final UserMapper userMapper;
    private final TokenHelper tokenHelper;

    @PostConstruct
    void init() {
        phoneBloomFilter = bloomFilters.getLast();
    }

    @Override
    public LoginType getType() {
        return LoginType.PHONE;
    }

    @Override
    public AuthResult login(SignInRequest request) {
        String phoneNum = request.getIdentifier();
        String code = request.getCredential();
        if (!phoneBloomFilter.contains(phoneNum)) {
            return new AuthResult(false);
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhoneNum, phoneNum)
                .select(User::getUid, User::getNickName, User::getUserType, User::getStatus)
        );
        if (user == null) {
            return new AuthResult(false);
        }
        String uid = user.getUid();
        String nickName = user.getNickName();
        Integer userType = user.getUserType();
        Integer status = user.getStatus();
        String validCode = redisTemplate.opsForValue().get("Verify:PhoneCode" + phoneNum);
        if (validCode == null) {
            return new AuthResult(false);
        }
        if (!code.equals(validCode) || status != 1) {
            return new AuthResult(false);
        }
        String token = tokenHelper.genToken(uid, nickName, userType);
        return new AuthResult(true, uid, token);
    }
}
