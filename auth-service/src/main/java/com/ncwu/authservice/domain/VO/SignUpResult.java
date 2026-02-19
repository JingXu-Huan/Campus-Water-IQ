package com.ncwu.authservice.domain.VO;


import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/19
 */
@Data
@AllArgsConstructor
public class SignUpResult {
    boolean isSuccess;
    String uid;
    String nickName;
    String msg;
}
