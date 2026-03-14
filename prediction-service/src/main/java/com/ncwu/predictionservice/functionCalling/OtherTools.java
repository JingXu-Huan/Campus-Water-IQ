package com.ncwu.predictionservice.functionCalling;


import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 此类提供其他供LLM调用的工具
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OtherTools {

    @Tool("""
            此方法返回一个现在的时间。当你发现其他工具可能需要现在的时间时，你可以调用此工具。
            """)
    LocalDateTime getNow() {
        return LocalDateTime.now();
    }

    @Tool("""
            此工具可用于：格式化用水量显示（添加单位）
            当需要返回用水数据时，你可以使用此工具简单格式化。
            """)
    String formatUsage(Double usage) {
        return usage != null ? String.format("%.2f 立方米", usage) : "无数据";
    }

    @Tool("根据校区编号获取校区名称")
    String getCampusName(int campusCode) {
        return switch (campusCode) {
            case 1 -> "花园校区";
            case 2 -> "龙子湖校区";
            case 3 -> "江淮校区";
            default -> "未知校区";
        };
    }
}
