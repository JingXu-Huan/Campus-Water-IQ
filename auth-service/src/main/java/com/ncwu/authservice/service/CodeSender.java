package com.ncwu.authservice.service;


/**
 * 发送验证码
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/10
 */
public interface CodeSender {

    void sendMailCode(String toEmail);

    void sendPhoneCode(String phoneNum);
}
