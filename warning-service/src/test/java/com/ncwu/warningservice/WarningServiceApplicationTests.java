package com.ncwu.warningservice;

import com.ncwu.common.apis.common_service.EmailServiceInterFace;
import org.apache.dubbo.config.annotation.DubboReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WarningServiceApplicationTests {
    @DubboReference(version = "1.0.0", interfaceClass = EmailServiceInterFace.class)
    private EmailServiceInterFace emailServiceInterFace;
    @Test
    public void sendConde() throws Exception{
        emailServiceInterFace.sendVerificationCode("jingxushi13@gmail.com","123456");
    }


}
