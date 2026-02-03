package com.ncwu.common.service;

import com.ncwu.common.apis.common_service.EmailServiceInterFace;
import com.ncwu.common.enums.ErrorCode;
import com.ncwu.common.domain.vo.Result;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@DubboService(version = "1.0.0",interfaceClass = EmailServiceInterFace.class)
public class EmailService implements EmailServiceInterFace{
    private final JavaMailSender mailSender;
    private static final Pattern MAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");
    @Value("${spring.mail.fromEmail}")
    private String fromEmail;

    /**
     * 方法向指定邮箱发送验证码
     *
     * @param toEmail 指定邮箱
     * @param code    验证码
     * @throws MessagingException 邮件消息处理异常
     * @throws MailException      邮件发送异常
     */

    @Retryable(retryFor = {MessagingException.class, MailException.class},
            //默认重复三次
            backoff = @Backoff(delay = 5000))
    public Result<Boolean> sendVerificationCode(String toEmail, String code) throws MailException, MessagingException {
        if (!isValidEmail(toEmail)) {
            return Result.fail(false, ErrorCode.PARAM_VALIDATION_ERROR.code(), ErrorCode.PARAM_VALIDATION_ERROR.message());
        }
        //MimeMessage：支持多媒体内容的邮件消息对象，可以发送HTML格式邮件
        MimeMessage message = mailSender.createMimeMessage();
        //创建 MimeMessageHelper 辅助对象
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        //主题
        helper.setSubject("验证码");
        String content = buildContent(code);
        helper.setText(content, true);
        mailSender.send(message);
        return Result.ok(true);
    }

    /**
     * 方法向指定邮箱发送验证码
     *
     * @param subject 主题
     * @param toEmail 指定邮箱
     * @param content 内容
     * @throws MailException      邮件发送异常
     */
    @Retryable(retryFor = {MessagingException.class, MailException.class},
            //默认重复三次
            backoff = @Backoff(delay = 5000))
    public Result<Boolean> sendMail(String subject, String content, String toEmail) throws MailException, MessagingException {
        if (!isValidEmail(toEmail)) {
            return Result.fail(false, ErrorCode.PARAM_VALIDATION_ERROR.code(), ErrorCode.PARAM_VALIDATION_ERROR.message());
        }
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(message, true, "UTF-8");
        mimeMessageHelper.setFrom(fromEmail);
        mimeMessageHelper.setTo(toEmail);
        mimeMessageHelper.setSubject(subject);
        mimeMessageHelper.setText(content, true);
        mailSender.send(message);
        return Result.ok(true);
    }

    private String buildContent(String content) {
        return "<h2>您的验证码</h2>" + "<p>验证码：<strong style='font-size: 24px; color: #007bff;'>" + content + "</strong></p>" + "<p>5分钟内有效，请勿泄露。</p>";
    }

    private boolean isValidEmail(String email) {
        return email != null && MAIL_PATTERN.matcher(email).matches();
    }
}
