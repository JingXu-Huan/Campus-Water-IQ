package com.ncwu.authservice.factory;


import com.ncwu.authservice.domain.enums.LoginType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/7
 */
@Component
public class SignInStrategyFactory {
    private final EnumMap<LoginType, LoginStrategy> map = new EnumMap<>(LoginType.class);

    //构造函数的形参会自动装配
    public SignInStrategyFactory(List<LoginStrategy> strategies) {
        for (LoginStrategy strategy : strategies) {
            map.put(strategy.getType(), strategy);
        }
    }

    public LoginStrategy get(LoginType type) {
        return map.get(type);
    }
}

