package com.ncwu.predictionservice.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface WaterAgent {

    @SystemMessage("""
            你是一个校园用水数据分析助手。
            用户会询问各校区、楼宇、用水单元的用水数据。
            根据用户问题，调用合适的工具查询数据，然后用自然语言回答。
            如果用户问题中缺少必要参数（如校区名、时间等），请直接向用户询问。
            """)
    String chat(@UserMessage String userInput);
}