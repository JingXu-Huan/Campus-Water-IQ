package com.ncwu.predictionservice.config;


import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/5
 */
@Configuration
public class ModelConfig {
    String key = System.getenv("API_KEY");

    @Bean
    public ChatLanguageModel initModel() {
        return ZhipuAiChatModel.builder()
                .apiKey(key)
                .callTimeout(Duration.ofSeconds(60))
                .connectTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .readTimeout(Duration.ofSeconds(60))
                .build();
    }
}
