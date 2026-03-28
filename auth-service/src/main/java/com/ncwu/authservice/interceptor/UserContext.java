package com.ncwu.authservice.interceptor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 用户上下文信息
 * 使用 ThreadLocal 存储当前请求的用户信息
 *
 * @author jingxu
 * @since 2026/3/20
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> USER_ROLE = new ThreadLocal<>();

    public static void set(String userId, int role) {
        USER_ID.set(userId);
        USER_ROLE.set(role);
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static Integer getRole() {
        return USER_ROLE.get();
    }

    public static void clear() {
        USER_ID.remove();
        USER_ROLE.remove();
    }
}
