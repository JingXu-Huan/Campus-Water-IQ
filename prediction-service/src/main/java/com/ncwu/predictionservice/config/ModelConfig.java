package com.ncwu.predictionservice.config;


import com.ncwu.predictionservice.agent.WaterAgent;
import com.ncwu.predictionservice.functionCalling.IotDeviceTools;
import com.ncwu.predictionservice.functionCalling.OtherTools;
import com.ncwu.predictionservice.functionCalling.RepairTools;
import com.ncwu.predictionservice.functionCalling.WaterQueryTools;
import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/5
 */
@Configuration
@RequiredArgsConstructor
public class ModelConfig {
    String key = System.getenv("API_KEY");

    private final WaterQueryTools waterQueryTools;
    private final IotDeviceTools iotDeviceTools;
    private final RepairTools repairTools;
    private final OtherTools otherTools;

    @Bean
    public ChatLanguageModel initModel() {
        return ZhipuAiChatModel
                .builder()
                .apiKey(key)
                .model("glm-4-plus")
                .callTimeout(Duration.ofSeconds(60))
                .connectTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .readTimeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public WaterAgent waterAgent(ChatLanguageModel chatLanguageModel) {
        return AiServices.builder(WaterAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .tools(waterQueryTools,iotDeviceTools,repairTools,otherTools)   // ← Tools在这里注册
                .build();
    }
}
