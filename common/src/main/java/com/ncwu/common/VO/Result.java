package com.ncwu.common.VO;

import lombok.Data;

/**
 * 所有微服务的统一响应体
 *
 * @author jingxu
 * @version 1.1.0
 * @since 2025/12/25
 */
@Data
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    private Result() {
    }

    public static <T> Result<T> ok(T data) {
        return ok(data, 200, "success");
    }

    public static <T> Result<T> ok(T data, Integer code, String message) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        result.data = data;
        return result;
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return fail(null, code, message);
    }

    public static <T> Result<T> fail(T data, Integer code, String message) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        result.data = data;
        return result;
    }
}
