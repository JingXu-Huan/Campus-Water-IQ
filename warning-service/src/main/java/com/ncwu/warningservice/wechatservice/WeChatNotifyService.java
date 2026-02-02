package com.ncwu.warningservice.wechatservice;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class WeChatNotifyService {
    private static final String WEBHOOK_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=5fc78dbc-3631-46d1-9c5f-fc6399729a75";

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
