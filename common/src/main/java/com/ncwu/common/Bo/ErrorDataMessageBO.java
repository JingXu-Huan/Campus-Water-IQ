package com.ncwu.common.Bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 异常数据封装类
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/24
 */

@Data
public class ErrorDataMessageBO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    String deviceId;
    String payLoad;
    String message = "数据异常";
}
