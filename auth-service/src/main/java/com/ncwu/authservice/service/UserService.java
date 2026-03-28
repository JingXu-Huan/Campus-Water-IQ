package com.ncwu.authservice.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ncwu.authservice.domain.DTO.SignUpRequest;
import com.ncwu.authservice.domain.VO.AuthResult;
import com.ncwu.authservice.domain.DTO.SignInRequest;
import com.ncwu.authservice.domain.VO.SignUpResult;
import com.ncwu.authservice.domain.entity.User;
import com.ncwu.common.domain.vo.Result;
import org.springframework.web.multipart.MultipartFile;


/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/7
 */
public interface UserService extends IService<User> {
    AuthResult signIn(SignInRequest request);

    SignUpResult signUp(SignUpRequest request);

    Result<Boolean> uploadAvatar(MultipartFile file, String uid);

    Result<String> getAvatar(String uid);

    Result<Boolean> changeNickName(String newName,String uid);

    Result<Boolean> changePwd(String oldPwd,String newPwd, String uid);

    Result<Boolean> bindingDevice(String uid, String deviceCode);

    Result<Boolean> forbiddenSomeUser(String uid);

    Result<Boolean> unforbiddenSomeUser(String uid);

    Result<Boolean> changeRole(String uid, Integer newRole);

    Result<Boolean> foundPwd(String code);
}
