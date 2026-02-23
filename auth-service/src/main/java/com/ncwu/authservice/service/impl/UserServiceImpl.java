package com.ncwu.authservice.service.impl;


import com.aliyun.oss.OSS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncwu.authservice.domain.DTO.SignUpRequest;
import com.ncwu.authservice.domain.VO.AuthResult;
import com.ncwu.authservice.domain.VO.SignUpResult;
import com.ncwu.authservice.domain.enums.LoginType;
import com.ncwu.authservice.domain.DTO.SignInRequest;
import com.ncwu.authservice.domain.entity.User;
import com.ncwu.authservice.domain.enums.SignUpType;
import com.ncwu.authservice.factory.login.LoginStrategy;
import com.ncwu.authservice.factory.login.LoginStrategyFactory;
import com.ncwu.authservice.factory.signup.SignUpStrategy;
import com.ncwu.authservice.factory.signup.SignUpStrategyFactory;
import com.ncwu.authservice.mapper.UserMapper;
import com.ncwu.authservice.service.UserService;
import com.ncwu.common.domain.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/7
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final LoginStrategyFactory loginStrategyFactory;
    private final SignUpStrategyFactory signUpStrategyFactory;
    private final OSS OSSClient;

    @Override
    public AuthResult signIn(SignInRequest request) {
        LoginType type = request.getType();
        LoginStrategy strategy = loginStrategyFactory.get(type);
        return strategy.login(request);
    }

    @Override
    public SignUpResult signUp(SignUpRequest request) {
        SignUpType type = request.getType();
        SignUpStrategy strategy = signUpStrategyFactory.get(type);
        return strategy.signUp(request);
    }

    @Override
    public Result<Boolean> uploadAvatar(MultipartFile file, String uid) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = "avatar/" + UUID.randomUUID() + suffix;
            try {
                InputStream inputStream = file.getInputStream();
                OSSClient.putObject("jingxu", fileName, inputStream);
                String url = "https://" + "jingxu" + "." + "oss-cn-beijing.aliyuncs.com" + "/" + fileName;
                // 保存到数据库
                boolean update = this.lambdaUpdate().eq(User::getUid, uid).set(User::getAvatar, url).update();
                if (update) {
                    return Result.ok(true);
                }
            } catch (IOException e) {
                log.error("文件上传失败: {}", e.getMessage());
                return Result.fail(false);
            } catch (Exception e) {
                log.error("OSS上传失败: {}", e.getMessage());
                // 如果OSS上传失败，可以返回一个默认头像或者本地存储的路径
                String defaultAvatar = "https://default-avatar-url.jpg";
                boolean update = this.lambdaUpdate().eq(User::getUid, uid).set(User::getAvatar, defaultAvatar).update();
                if (update) {
                    return Result.ok(true);
                }
                return Result.fail(false);
            }
        }
        return Result.fail(false);
    }
}
