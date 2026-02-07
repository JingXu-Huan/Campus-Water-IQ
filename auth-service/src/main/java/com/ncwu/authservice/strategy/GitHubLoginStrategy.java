package com.ncwu.authservice.strategy;


import com.ncwu.authservice.entity.AuthResult;
import com.ncwu.authservice.entity.LoginType;
import com.ncwu.authservice.entity.SignInRequest;
import com.ncwu.authservice.factory.LoginStrategy;
import org.springframework.stereotype.Component;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/7
 */
@Component
public class GitHubLoginStrategy implements LoginStrategy {
    @Override
    public LoginType getType() {
        return LoginType.GITHUB;
    }

    @Override
    public AuthResult login(SignInRequest request) {
        return null;
    }
}
