package com.ncwu.authservice.factory.signup;


import com.ncwu.authservice.domain.enums.SignUpType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/7
 */
@Component
public class SignUpStrategyFactory {
    private final EnumMap<SignUpType, SignUpStrategy> map = new EnumMap<>(SignUpType.class);

    //构造函数的形参会自动装配
    public SignUpStrategyFactory(List<SignUpStrategy> strategies) {
        for (SignUpStrategy strategy: strategies) {
            map.put(strategy.getType(), strategy);
        }
    }

    public SignUpStrategy get(SignUpType type) {
        return map.get(type);
    }
}

