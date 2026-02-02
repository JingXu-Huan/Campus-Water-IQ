package emailservice;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Retryable(
            value = MessagingException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 5000)
    )
    public void sendVerificationCode(String toEmail, String code) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("验证码");
            String content = buildContent(code);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("邮件发送失败, e");
        }
    }

    private String buildContent(String content) {
        return "<h2>您的验证码</h2>" +
                "<p>验证码：<strong style='font-size: 24px; color: #007bff;'>" + content + "</strong></p>" +
                "<p>5分钟内有效，请勿泄露。</p>";
    }
}
