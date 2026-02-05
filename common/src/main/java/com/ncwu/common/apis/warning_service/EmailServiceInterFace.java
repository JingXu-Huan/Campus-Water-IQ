package com.ncwu.common.apis.warning_service;


import com.ncwu.common.domain.vo.Result;
import jakarta.mail.MessagingException;
import org.springframework.mail.MailException;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/3
 */
public interface EmailServiceInterFace {
    Result<Boolean> sendVerificationCode(String toEmail, String code) throws MessagingException, MailException;

    Result<Boolean> sendMail(String subject, String content, String toEmail) throws MessagingException,MailException;
}
