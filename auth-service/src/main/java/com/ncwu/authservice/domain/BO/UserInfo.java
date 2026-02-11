package com.ncwu.authservice.domain.BO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/10
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    Integer userType;
    String nickName;
    String uid;
    Integer status;
    String validPassword;
}
