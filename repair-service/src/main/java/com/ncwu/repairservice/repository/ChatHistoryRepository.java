package com.ncwu.repairservice.repository;


import com.ncwu.repairservice.entity.vo.MessageVO;

import java.util.List;

public interface ChatHistoryRepository {

    void save(String type, String chatId);

    List<String> getChatIds(String type);

    List<MessageVO> getChatHistory(String type, String chatId);
}
