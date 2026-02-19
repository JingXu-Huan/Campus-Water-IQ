package com.ncwu.authservice.factory.login;

import com.ncwu.authservice.domain.VO.AuthResult;
import com.ncwu.authservice.domain.enums.LoginType;
import com.ncwu.authservice.domain.DTO.SignInRequest;


public interface LoginStrategy {
    LoginType getType();

    AuthResult login(SignInRequest request);
}