package com.ncwu.iotdevice.AOP.Aspects;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * 初始化特定 lua 脚本
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/2
 */
@Aspect
@Component
public class InitLuaScript {
    public static DefaultRedisScript<Long> Lua_script;

    @Before("@annotation(initLuaScript)")
    public void beforeMethod(com.ncwu.iotdevice.AOP.annotation.InitLuaScript initLuaScript) {
        if (Lua_script == null) {
            Lua_script = new DefaultRedisScript<>();
            Lua_script.setLocation(new ClassPathResource(initLuaScript.value()));
            Lua_script.setResultType(Long.class);
            System.out.println("Lua脚本初始化完成: " + initLuaScript.value());
        }
    }
}