package com.ncwu.repairservice.controller;


import com.ncwu.repairservice.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 智能报修/预约表 前端控制器
 * </p>
 *
 * @author author
 * @since 2026-01-15
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class DeviceReservationController {
    public final ChatClient serviceChatClient;
    public final ChatHistoryRepository chatHistoryRepository;
    @RequestMapping("/service")
    public String service(String prompt, String chatId) {
        // 1.保存会话id
        chatHistoryRepository.save("service", chatId);
        // 2.请求模型
        return serviceChatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .content();
    }
}
