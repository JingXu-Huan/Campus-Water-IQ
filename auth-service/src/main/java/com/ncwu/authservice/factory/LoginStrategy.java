package com.ncwu.authservice.factory;

import com.ncwu.authservice.entity.AuthResult;
import com.ncwu.authservice.entity.LoginType;
import com.ncwu.authservice.entity.SignInRequest;


public interface LoginStrategy {
    LoginType getType();

    AuthResult login(SignInRequest request);
}