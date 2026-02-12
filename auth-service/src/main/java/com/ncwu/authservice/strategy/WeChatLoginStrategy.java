package com.ncwu.authservice.strategy;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncwu.authservice.domain.VO.AuthResult;
import com.ncwu.authservice.domain.enums.LoginType;
import com.ncwu.authservice.domain.DTO.SignInRequest;
import com.ncwu.authservice.domain.entity.User;
import com.ncwu.authservice.domain.BO.UserInfo;
import com.ncwu.authservice.exception.DeserializationFailedException;
import com.ncwu.authservice.exception.WeChatOAuthException;
import com.ncwu.authservice.factory.LoginStrategy;
import com.ncwu.authservice.mapper.UserMapper;
import com.ncwu.authservice.service.WeChatOAuthService;
import com.ncwu.authservice.service.TokenHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.ncwu.authservice.util.Utils.genUid;

/**
 * WeChat登录策略
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeChatLoginStrategy implements LoginStrategy {

    private final WeChatOAuthService weChatOAuthService;
    private final UserMapper userMapper;
    private final TokenHelper tokenHelper;
    private final StringRedisTemplate redisTemplate;
    private final List<RBloomFilter<String>> bloomFilters;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public LoginType getType() {
        return LoginType.WECHAT;
    }

    @Override
    public AuthResult login(SignInRequest request) {
        //注意 使用oauth登录不用传递 request.getIdentifier();
        String code = request.getCredential();
        if (code == null || code.trim().isEmpty()) {
            log.warn("WeChat登录失败: 授权码为空");
            return new AuthResult(false);
        }

        try {
            // 1. 使用授权码获取用户信息
            WeChatOAuthService.WeChatUserInfo weChatUser = weChatOAuthService.getUserInfoByCode(code);
            if (weChatUser == null || weChatUser.getOpenId() == null) {
                log.warn("WeChat登录失败: 无法获取用户信息");
                return new AuthResult(false);
            }
            String openId = weChatUser.getOpenId();
            // 2. 查询用户是否已绑定WeChat账号
            User user = findUserByWeChatOpenId(openId);
            if (user == null) {
                // 用户未绑定WeChat，创建新用户
                return handleNewWeChatUser(weChatUser);
            }
            // 3. 验证用户状态并生成令牌
            return authenticateUser(user);

        } catch (WeChatOAuthException e) {
            log.error("WeChat OAuth认证失败: {}", e.getMessage());
            return new AuthResult(false);
        } catch (Exception e) {
            log.error("WeChat登录过程中发生异常", e);
            return new AuthResult(false);
        }
    }

    /**
     * 根据 WeChat OpenID查找用户
     */
    private User findUserByWeChatOpenId(String openId) {
        // 先查询缓存
        String userInfo = redisTemplate.opsForValue().get("UserInfo:WeChat:" + openId);
        if (userInfo != null) {
            try {
                UserInfo info = objectMapper.readValue(userInfo, UserInfo.class);
                return userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getUid, info.getUid())
                        .select(User::getUid, User::getNickName, User::getUserType, User::getStatus));
            } catch (JsonProcessingException e) {
                log.error("反序列化WeChat用户缓存失败", e);
                throw new DeserializationFailedException("反序列化失败");
            }
        }
        // 查询数据库
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getWechatOpenId, openId)
                .select(User::getUid, User::getNickName, User::getUserType, User::getStatus));
        if (user != null) {
            // 缓存用户信息
            UserInfo info = new UserInfo(
                    user.getUserType(),
                    user.getNickName(),
                    user.getUid(),
                    user.getStatus(),
                    null
            );
            try {
                String jsonInfo = objectMapper.writeValueAsString(info);
                redisTemplate.opsForValue().set("UserInfo:WeChat:" + openId, jsonInfo,
                        300 + ThreadLocalRandom.current().nextInt(30), TimeUnit.SECONDS);
            } catch (JsonProcessingException e) {
                log.error("序列化WeChat用户缓存失败", e);
            }
        }

        return user;
    }

    /**
     * 处理新的WeChat用户
     */
    private AuthResult handleNewWeChatUser(WeChatOAuthService.WeChatUserInfo weChatUser) {
        // 自动创建新用户
        log.info("WeChat用户 {} 未绑定系统账号，自动创建新用户", weChatUser.getNickname());

        try {
            User newUser = new User();
            newUser.setNickName(weChatUser.getNickname());
            newUser.setWechatOpenId(weChatUser.getOpenId());
            newUser.setPhoneNum(null);
            newUser.setEmail(null);
            //生成Uid并保存
            newUser.setUid(genUid(1));
            newUser.setUserType(1); // 普通用户
            newUser.setStatus(1);   // 正常状态
            // 生成OAuth用户的随机密码
            String randomPassword = generateOAuthPassword();
            newUser.setPassword(passwordEncoder.encode(randomPassword));
            // 保存用户到数据库
            int result = userMapper.insert(newUser);
            if (result <= 0) {
                log.error("创建WeChat用户失败");
                return new AuthResult(false);
            }
            log.info("WeChat用户 {} 创建成功，UID: {}", weChatUser.getNickname(), newUser.getUid());
            // 生成登录令牌
            String token = tokenHelper.genToken(newUser.getUid(), newUser.getNickName(), newUser.getUserType());
            return new AuthResult(true, newUser.getUid(), token);
        } catch (Exception e) {
            log.error("创建WeChat用户时发生异常", e);
            return new AuthResult(false);
        }
    }

    /**
     * 生成OAuth用户的随机密码
     */
    private String generateOAuthPassword() {
        // 生成24位强密码：包含大小写字母、数字和特殊字符
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 24; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return "OAUTH_" + password;
    }

    /**
     * 认证用户并生成令牌
     */
    private AuthResult authenticateUser(User user) {
        if (user.getStatus() != 1) {
            log.warn("用户 {} 状态异常: {}", user.getUid(), user.getStatus());
            return new AuthResult(false);
        }
        String token = tokenHelper.genToken(user.getUid(), user.getNickName(), user.getUserType());
        log.info("WeChat用户 {} 登录成功", user.getUid());
        return new AuthResult(true, user.getUid(), token);
    }
}
