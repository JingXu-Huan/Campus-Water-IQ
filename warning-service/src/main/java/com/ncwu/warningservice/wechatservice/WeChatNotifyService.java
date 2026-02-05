package com.ncwu.warningservice.wechatservice;

import com.ncwu.common.apis.warning_service.WeChatNotifyInterFace;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@DubboService(version = "1.0.0", interfaceClass = WeChatNotifyInterFace.class)
public class WeChatNotifyService implements WeChatNotifyInterFace {
    private static final String WEBHOOK_URL;

    static {
        WEBHOOK_URL = System.getenv("WECHAT_WEBHOOK_URL");
        if (WEBHOOK_URL == null || WEBHOOK_URL.isBlank()) {
            throw new IllegalStateException("环境变量 WECHAT_WEBHOOK_URL 未配置");
        }
    }

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendMdText(String deviceCode, String level, String desc, String time, String suggestion) {
        Map<String, Object> body = new HashMap<>();
        body.put("msgtype", "markdown");
        Map<String, String> markdown = new HashMap<>();
        markdown.put(
                "content",
                String.format(
                        """
                                ### 🚨 设备告警通知
                                
                                > **设备号**：`%s` \s
                                > **告警等级**：<font color="warning">%s</font> \s
                                > **告警描述**：%s
                                
                                ---
                                
                                📅 **发生时间**：%s
                                **处理建议**：%s""",
                        deviceCode,
                        level,
                        desc,
                        time,
                        suggestion
                )
        );
        body.put("markdown", markdown);

        restTemplate.postForObject(WEBHOOK_URL, body, String.class);
    }

    @Override
    public void sendText(String content) {
        Map<String, Object> body = new HashMap<>();
        body.put("msgtype", "text");
        
        Map<String, String> text = new HashMap<>();
        text.put("content", content);
        
        body.put("text", text);
        restTemplate.postForObject(WEBHOOK_URL, body, String.class);
    }

}
