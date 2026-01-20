package com.ncwu.common.validator;


import com.ncwu.common.validator.annotations.Bloom;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/18
 */
@Component
@RequiredArgsConstructor
public class BloomFilterValidator implements ConstraintValidator<Bloom, List<String>> {
    private final RedissonClient redissonClient;

    @Override
    public boolean isValid(List<String> list, ConstraintValidatorContext constraintValidatorContext) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("device:bloom");
        boolean isValid = bloomFilter.contains(list) == list.size();
        
        if (!isValid) {
            // 禁用默认错误信息，使用自定义信息
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext.buildConstraintViolationWithTemplate(
                constraintValidatorContext.getDefaultConstraintMessageTemplate()
            ).addConstraintViolation();
        }
        
        return isValid;
    }
}
