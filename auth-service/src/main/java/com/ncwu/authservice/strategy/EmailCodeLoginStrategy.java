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
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/7
 */
@Component
@RequiredArgsConstructor
public class EmailCodeLoginStrategy implements LoginStrategy {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    @DubboReference(version = "1.0.0")
    private EmailServiceInterFace emailServiceInterFace;

    private final UserMapper userMapper;
    private final TokenHelper tokenHelper;
    private final StringRedisTemplate redisTemplate;
    private final List<RBloomFilter<String>> bloomFilters;
    private RBloomFilter<String> emailBloomFilter;

    //在自动装配完成之后，自动执行
    @PostConstruct
    void init(){
        emailBloomFilter = bloomFilters.getLast();
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
        //引入布隆过滤器，防止缓存穿透(必做,在初始化的时候执行一次全量加入过滤器)
        if (!isValidEmail(email) || !emailBloomFilter.contains(email)) {
            return new AuthResult(false);
        }

        //todo 优化这里的查询性能，不要每次查询数据库
        //todo 引入缓存
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .select(User::getUid, User::getNickName, User::getUserType, User::getStatus)
        );
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
        if (validCode == null) {
            return new AuthResult(false);
        }
        if (code.equals(validCode)) {
            String token = tokenHelper.genToken(uid, nickName, userType);
            return new AuthResult(true, uid, token);
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
}
