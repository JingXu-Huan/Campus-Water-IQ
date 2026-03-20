package com.ncwu.common.annotation;

import java.lang.annotation.*;

/**
 * 角色权限注解
 * 用于标注接口需要什么角色才能访问
 *
 * @author jingxu
 * @since 2026/3/20
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /**
     * 需要什么角色：1=普通用户, 2=运维, 3=管理员
     */
    int[] value() default {};

    /**
     * 角色名称（可选，方便阅读）
     */
    String[] names() default {};
}
