package com.ncwu.authservice.strategy;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ncwu.authservice.entity.AuthResult;
import com.ncwu.authservice.entity.LoginType;
import com.ncwu.authservice.entity.SignInRequest;
import com.ncwu.authservice.entity.User;
import com.ncwu.authservice.factory.LoginStrategy;
import com.ncwu.authservice.mapper.UserMapper;
import com.ncwu.authservice.service.TokenHelper;
import com.ncwu.common.apis.warning_service.EmailServiceInterFace;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/7
 */
@Component
@RequiredArgsConstructor
public class EmailCodeLoginStrategy implements LoginStrategy {
    @DubboReference(version = "1.0.0")
    private EmailServiceInterFace emailServiceInterFace;

    private final UserMapper userMapper;
    private final TokenHelper tokenHelper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public LoginType getType() {
        return LoginType.EMAIL;
    }

    @Override
    public AuthResult login(SignInRequest request) {
        //todo 校验邮箱合法
        String email = request.getIdentifier();
        String code = request.getCredential();
        //todo 优化这里的查询性能，不要每次查询数据库
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            return new AuthResult(false);
        }
        Integer userType = user.getUserType();
        String nickName = user.getNickName();
        String uid = user.getUid();
        Integer status = user.getStatus();
        if (status != 1) {
            return new AuthResult(false);
        }
        String validCode = redisTemplate.opsForValue().get("Verify:EmailCode" + email);
        if (code.equals(validCode)) {
            String token = tokenHelper.genToken(uid, nickName, userType);
            return new AuthResult(true, uid, token);
        } else {
            return new AuthResult(false);
        }

    }
}
