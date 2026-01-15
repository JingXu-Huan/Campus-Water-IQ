package com.ncwu.repairservice.controller;

import com.ncwu.repairservice.entity.vo.MessageVO;
import com.ncwu.repairservice.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ai/history")
@RequiredArgsConstructor
public class ChatHistoryController {
    private final ChatHistoryRepository chatHistoryRepository;
    @GetMapping("{type}")
    public List<String> getChatIdS(@PathVariable("type")String type) {
        return chatHistoryRepository.getChatIds(type);
    }
    @GetMapping("{type}/{chatId}")
    public List<MessageVO> getChatHistory(@PathVariable("type") String type, @PathVariable("chatId") String chatId) {
        return chatHistoryRepository.getChatHistory(type,chatId);
    }
 }
