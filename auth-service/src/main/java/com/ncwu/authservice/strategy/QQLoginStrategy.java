package com.ncwu.authservice.strategy;


import com.ncwu.authservice.domain.VO.AuthResult;
import com.ncwu.authservice.domain.enums.LoginType;
import com.ncwu.authservice.domain.DTO.SignInRequest;
import com.ncwu.authservice.factory.LoginStrategy;
import org.springframework.stereotype.Component;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/7
 */
@Component
public class QQLoginStrategy implements LoginStrategy {
    @Override
    public LoginType getType() {
        return LoginType.QQ;
    }

    @Override
    public AuthResult login(SignInRequest request) {
        return null;
    }
}
