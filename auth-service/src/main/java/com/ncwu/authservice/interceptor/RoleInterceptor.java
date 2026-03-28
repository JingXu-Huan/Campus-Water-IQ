package com.ncwu.authservice.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncwu.common.annotation.RequireRole;
import com.ncwu.common.domain.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 角色权限拦截器
 * 从网关传递的请求头中获取用户信息，校验接口权限
 *
 * @author jingxu
 * @since 2026/3/20
 */
@Slf4j
@Component
public class RoleInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        // 只拦截 Controller 方法
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 检查方法上是否有 RequireRole 注解
        RequireRole annotation = handlerMethod.getMethodAnnotation(RequireRole.class);

        // 没有注解 → 放行（允许所有人访问）
        if (annotation == null) {
            return true;
        }

        // 获取请求头中的用户信息（网关传递）
        String userId = request.getHeader("X-User-Id");
        String userRoleStr = request.getHeader("X-User-Role");

        // 没有登录信息 → 拒绝
        if (userId == null || userRoleStr == null) {
            writeUnauthorized(response, "未登录或Token无效");
            return false;
        }

        int userRole;
        try {
            userRole = Integer.parseInt(userRoleStr);
        } catch (NumberFormatException e) {
            writeUnauthorized(response, "角色信息错误");
            return false;
        }

        // 检查权限
        int[] requiredRoles = annotation.value();
        for (int required : requiredRoles) {
            if (userRole == required) {
                // 匹配成功，存入 UserContext 备用
                UserContext.set(userId, userRole);
                return true;
            }
        }

        // 权限不足
        String requiredNames = String.join("/", annotation.names());
        writeForbidden(response, "权限不足，此操作需要角色: " + requiredNames);
        return false;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception ex) {
        // 请求完成后清理 ThreadLocal
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(401, msg)));
    }

    private void writeForbidden(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(403, msg)));
    }
}
