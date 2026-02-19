package com.ncwu.authservice.factory.signup;


import com.ncwu.authservice.domain.DTO.SignUpRequest;
import com.ncwu.authservice.domain.VO.SignUpResult;
import com.ncwu.authservice.domain.enums.SignUpType;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/10
 */
public interface SignUpStrategy {
    SignUpType getType();

    SignUpResult signUp(SignUpRequest signUpRequest);
}
