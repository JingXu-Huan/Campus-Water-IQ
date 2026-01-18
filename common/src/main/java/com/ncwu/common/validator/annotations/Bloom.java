package com.ncwu.common.validator.annotations;


import com.ncwu.common.validator.BloomFilterValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/18
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Constraint(validatedBy = BloomFilterValidator.class)
public @interface Bloom {
    String message() default "设备号不存在";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
