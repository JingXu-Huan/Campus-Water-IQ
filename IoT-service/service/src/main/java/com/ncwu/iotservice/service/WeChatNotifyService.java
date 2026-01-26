package com.ncwu.iotservice.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WeChatNotifyService {
    private static final String WEBHOOK_URL;

    static {
        WEBHOOK_URL = System.getenv("WECHAT_WEBHOOK_URL");
        if (WEBHOOK_URL == null || WEBHOOK_URL.isBlank()) {
            throw new IllegalStateException("环境变量 WECHAT_WEBHOOK_URL 未配置");
        }
    }

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendText(String content) {
        Map<String, Object> body = new HashMap<>();
        body.put("msgtype", "text");

        Map<String, String> text = new HashMap<>();
        text.put("content", content);

        body.put("text", text);

        restTemplate.postForObject(WEBHOOK_URL, body, String.class);
    }
}
