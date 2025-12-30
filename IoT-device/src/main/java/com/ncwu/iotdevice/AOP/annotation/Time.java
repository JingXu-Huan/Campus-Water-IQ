package com.ncwu.iotdevice.AOP.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于获取方法耗时
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/30
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Time {
}
