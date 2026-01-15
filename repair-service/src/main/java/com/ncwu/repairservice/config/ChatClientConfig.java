package com.ncwu.repairservice.config;

import com.ncwu.repairservice.tools.RepairTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.ncwu.repairservice.constants.SystemConstants.REPAIR_SERVICE_SYSTEM;

@Configuration
public class ChatClientConfig {
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory
                .builder()
                .maxMessages(100)
                .build();
    }
    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel, ChatMemory chatMemory, RepairTools tools) {
        return ChatClient.builder(chatModel)
                .defaultSystem(REPAIR_SERVICE_SYSTEM)
                .defaultTools(tools)
                .defaultAdvisors(new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
