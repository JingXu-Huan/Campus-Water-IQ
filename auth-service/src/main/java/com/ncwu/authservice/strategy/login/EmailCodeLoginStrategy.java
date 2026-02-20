package com.ncwu.authservice.strategy.login;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncwu.authservice.domain.VO.AuthResult;
import com.ncwu.authservice.domain.enums.LoginType;
import com.ncwu.authservice.domain.DTO.SignInRequest;
import com.ncwu.authservice.domain.entity.User;
import com.ncwu.authservice.domain.BO.UserInfo;
import com.ncwu.authservice.exception.DeserializationFailedException;
import com.ncwu.authservice.factory.login.LoginStrategy;
import com.ncwu.authservice.mapper.UserMapper;
import com.ncwu.authservice.service.TokenHelper;
import com.ncwu.authservice.service.CodeSender;
import com.ncwu.authservice.util.Utils;
import com.ncwu.common.apis.warning_service.EmailServiceInterFace;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.redisson.api.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.ncwu.authservice.util.Utils.genValidCode;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/7
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailCodeLoginStrategy implements LoginStrategy, CodeSender {
    //邮箱正则表达式
    private static final Pattern EMAIL_PATTERN = Pattern
            .compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    @DubboReference(version = "1.0.0", interfaceClass = EmailServiceInterFace.class)
    private EmailServiceInterFace emailServiceInterFace;

    private final UserMapper userMapper;
    private final TokenHelper tokenHelper;
    private final StringRedisTemplate redisTemplate;
    private final List<RBloomFilter<String>> bloomFilters;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;
    private final Utils utils;
    private RBloomFilter<String> emailBloomFilter;

    //在自动装配完成之后，自动执行
    @PostConstruct
    void init() {
        emailBloomFilter = bloomFilters.getFirst();
    }

    @Override
    public LoginType getType() {
        return LoginType.EMAIL;
    }

    @Override
    public AuthResult login(SignInRequest request) {
        String email = request.getIdentifier();
        String code = request.getCredential();
        // 校验邮箱格式
        //引入布隆过滤器，防止缓存穿透
        boolean contains = emailBloomFilter.contains(email);
        if (!isValidEmail(email) || !contains) {
            log.warn("邮箱验证失败: 格式={}, 布隆过滤器={}", isValidEmail(email), contains);
            return new AuthResult(false);
        }
        //先查询缓存
        String userInfo = redisTemplate.opsForValue().get("UserInfo:" + email);
        if (userInfo != null) {
            try {
                UserInfo info = objectMapper.readValue(userInfo, UserInfo.class);
                return check(info.getStatus(), email, code, info.getUid(), info.getNickName(), info.getUserType());
            } catch (JsonProcessingException e) {
                throw new DeserializationFailedException("反序列化失败");
            }
        } else {
            //查询数据库
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, email)
                    .eq(User::getGithubId, null)
                    .select(User::getUid, User::getNickName, User::getUserType, User::getStatus)
            );
            if (user == null) {
                return new AuthResult(false);
            }
            UserInfo info = new UserInfo(
                    user.getUserType(),
                    user.getNickName(),
                    user.getUid(),
                    user.getStatus(),
                    null
            );
            String jsonInfo;
            try {
                jsonInfo = objectMapper.writeValueAsString(info);
            } catch (JsonProcessingException e) {
                throw new DeserializationFailedException("序列化异常");
            }
            //写入缓存
            redisTemplate.opsForValue().set("UserInfo:" + email, jsonInfo,
                    300 + ThreadLocalRandom.current().nextInt(60), TimeUnit.SECONDS);
            //只查询四个字段
            Integer userType = user.getUserType();
            String nickName = user.getNickName();
            String uid = user.getUid();
            Integer status = user.getStatus();
            return check(status, email, code, uid, nickName, userType);
        }
    }

    private AuthResult check(Integer status, String email, String code, String uid, String nickName, Integer userType) {
        if (status != 1) {
            return new AuthResult(false);
        }
        String validCode = redisTemplate.opsForValue().get("Verify:EmailCode:" + email);
        if (validCode == null) {
            return new AuthResult(false);
        }
        if (code.equals(validCode)) {
            String token = tokenHelper.genToken(uid, nickName, userType);
            return new AuthResult(true, uid, token,nickName);
        } else {
            return new AuthResult(false);
        }
    }

    /**
     * 验证邮箱格式是否合法
     *
     * @param email 邮箱地址
     * @return true-合法，false-非法
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    @Override
    public void sendMailCode(String toEmail) {
        utils.sendValidCodeToMail(toEmail, redissonClient, redisTemplate);
    }

    /**
     * 向指定手机号发送SMS消息
     *
     * @param phoneNum 要发送验证码的手机号
     */
    @Override
    public void sendPhoneCode(String phoneNum) {
        String code = genValidCode();
        //todo 接口防刷
        //todo 接入手机短信服务
        log.debug("手机登陆验证码{}", code);
        //todo 异常消息队列处理
    }
}
