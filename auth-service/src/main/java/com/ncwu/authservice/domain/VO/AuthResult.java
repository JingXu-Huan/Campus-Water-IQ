package com.ncwu.authservice.domain.VO;


import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/7
 */
@Data
@AllArgsConstructor
public class AuthResult {
    boolean isSuccess;
    String uid;
    String token;
    String nickName;
    String avatarURL;

    public AuthResult(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }
}
