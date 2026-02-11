package com.ncwu.authservice.domain.DTO;

import com.ncwu.authservice.domain.enums.LoginType;
import lombok.Data;

@Data
public class SignInRequest {
    private LoginType type;      // PASSWORD / PHONE / EMAIL / WECHAT / QQ / GITHUB
    private String identifier;   // username / phone / email / openId
    private String credential;   // password / code / null
}