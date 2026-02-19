package com.ncwu.authservice.strategy;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ncwu.authservice.domain.DTO.SignUpRequest;
import com.ncwu.authservice.domain.VO.SignUpResult;
import com.ncwu.authservice.domain.entity.User;
import com.ncwu.authservice.domain.enums.SignUpType;
import com.ncwu.authservice.factory.signup.SignUpStrategy;
import com.ncwu.authservice.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

import static com.ncwu.authservice.util.Utils.genUid;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCodeSignUpStrategy implements SignUpStrategy {
    //邮箱正则表达式
    private static final Pattern EMAIL_PATTERN = Pattern
            .compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    private final StringRedisTemplate redisTemplate;
    private final UserMapper userMapper;

    @Override
    public SignUpType getType() {
        return SignUpType.MAIL_AND_CODE;
    }

    @Override
    public SignUpResult signUp(SignUpRequest signUpRequest) {
        String toEmail = signUpRequest.getIdentifier();
        if (!isValidEmail(toEmail)) {
            return new SignUpResult(false, "", "", "");
        }
        String code = signUpRequest.getCredential();
        String validCode = redisTemplate.opsForValue().get("Verify:EmailCode:" + toEmail);
        if (!code.equals(validCode)) {
            return new SignUpResult(false, "", "", "验证码错误");
        }
        //查询用户是否已经存在
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, toEmail));
        if (user != null) {
            return new SignUpResult(false, "", "", user.getNickName() + "已经注册，可以直接登录");
        }

        String uid = genUid(1);
        String nickname = signUpRequest.getNickname();
        // 前端已加密的密码，直接存储
        String pwd = signUpRequest.getPwd();
        User userEntity = new User();
        userEntity.setEmail(toEmail);
        userEntity.setNickName(nickname);
        userEntity.setPassword(pwd);
        userEntity.setUid(uid);
        userMapper.insert(userEntity);

        return new SignUpResult(true, uid, nickname, "注册成功，请登录");
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
}
