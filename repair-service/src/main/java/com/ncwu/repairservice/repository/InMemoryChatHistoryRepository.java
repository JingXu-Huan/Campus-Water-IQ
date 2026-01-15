package com.ncwu.repairservice.repository;

import com.ncwu.repairservice.entity.vo.MessageVO;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class InMemoryChatHistoryRepository implements ChatHistoryRepository{
    private final Map<String,List<String>> chatHistory = new HashMap<>();
    private final ChatMemory chatMemory;

    public InMemoryChatHistoryRepository(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    @Override
    public void save(String type, String chatId) {
//        if(!chatHistory.containsKey(type)) {
//            chatHistory.put(type,new ArrayList<>());
//        }
//        List<String> ids = chatHistory.get(type);
        List<String> ids = chatHistory.computeIfAbsent(type, k -> new ArrayList<>());
        if(ids.contains(chatId)) {
            return;
        }
        ids.add(chatId);
    }

    @Override
    public List<String> getChatIds(String type) {
//        List<String> chatIds = chatHistory.get(type);
//        return chatIds==null?new ArrayList<>():chatIds;
        return chatHistory.getOrDefault(type,new ArrayList<>());
    }

    @Override
    public List<MessageVO> getChatHistory(String type, String chatId) {
        List<Message> messages = chatMemory.get(chatId);
        if(messages==null) {
            return List.of();
        }
        return messages.stream().map(MessageVO::new).collect(Collectors.toList());
    }
}
