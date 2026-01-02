package com.ncwu.iotdevice.AOP.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 此注解用于方法,表示数据有概率不可信
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/2
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotCredible {
}
