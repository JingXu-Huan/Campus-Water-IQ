import com.ncwu.common.apis.warning_service.EmailServiceInterFace;
import com.ncwu.common.apis.warning_service.WeChatNotifyInterFace;
import com.ncwu.repairservice.RepairServiceApplication;
import org.apache.dubbo.config.annotation.DubboReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = RepairServiceApplication.class)
class RepairServiceApplicationTests {
    @DubboReference(version = "1.0.0", interfaceClass = EmailServiceInterFace.class)
    private EmailServiceInterFace emailServiceInterFace;

    @DubboReference(version = "1.0.0", interfaceClass = WeChatNotifyInterFace.class)
    private WeChatNotifyInterFace weChatNotifyInterFace;

    @Test
    public void sendConde() throws Exception{
        emailServiceInterFace.sendVerificationCode("jingxushi13@gmail.com","123456");
    }

    @Test
    public void sendText() throws Exception{
        weChatNotifyInterFace.sendText("This is jingxu. Hi there!");
    }
}
