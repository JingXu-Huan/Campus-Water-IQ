package com.ncwu.authservice.domain.DTO;

import com.ncwu.authservice.domain.enums.SignUpType;
import lombok.Data;

@Data
public class SignUpRequest {
    private SignUpType type;      //  / PHONE / EMAIL / GITHUB
    private String identifier;   // / phone / email / openId
    private String credential;   // password / code / null
    private String pwd;
    private String nickname;
}