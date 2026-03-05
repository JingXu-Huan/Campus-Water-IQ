package com.ncwu.predictionservice.impl;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncwu.common.domain.vo.Result;
import com.ncwu.predictionservice.AiService;
import com.ncwu.predictionservice.domain.UsageBO;
import com.ncwu.predictionservice.domain.vo.UsageVO;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatLanguageModel chatLanguageModel;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    String keyPrefix = "WaterPredictionUsage:";
    @Override
    public Result<UsageVO> predictTomorrowWaterUsage(List<Double> usage, int campus) {
        String json = redisTemplate.opsForValue().get(keyPrefix + campus);
        
        if (json == null || json.isEmpty()) {
            return generateAndCachePrediction(usage, campus);
        }
        
        UsageBO usageBO = parseCachedUsage(json);
        
        if (usageBO != null && !isCacheExpired(usageBO)) {
            return Result.ok(new UsageVO(campus, usageBO.getUsage()));
        }
        
        if (usageBO != null) {
            regeneratePredictionAsync(campus, usage);
            return Result.ok(new UsageVO(campus, usageBO.getUsage()));
        }
        
        return generateAndCachePrediction(usage, campus);
    }

    private double getRes(List<Double> usage) {
        try {
            String response = chatLanguageModel.chat(
                "Predict the next water usage value based on this data: " + usage.toString() +
                ". Return ONLY a single number without any explanation, text, or formatting. Example response: 209.25"
            );
            return Double.parseDouble(response.trim());
        } catch (NumberFormatException e) {
            log.error("Failed to parse AI response: {}", e.getMessage());
            throw new RuntimeException("Invalid AI response format", e);
        } catch (Exception e) {
            log.error("AI prediction failed: {}", e.getMessage());
            throw new RuntimeException("AI prediction failed", e);
        }
    }

    private Result<UsageVO> generateAndCachePrediction(List<Double> usage, int campus) {
        double predictedValue = getRes(usage);
        UsageBO usageBO = new UsageBO(predictedValue, LocalDateTime.now().plusMinutes(300));
        
        try {
            redisTemplate.opsForValue().set(keyPrefix + campus, objectMapper.writeValueAsString(usageBO));
        } catch (JsonProcessingException e) {
            log.error("Failed to cache prediction for campus {}: {}", campus, e.getMessage());
        }
        
        return Result.ok(new UsageVO(campus, predictedValue));
    }
    
    private UsageBO parseCachedUsage(String json) {
        try {
            return objectMapper.readValue(json, UsageBO.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse cached usage data: {}", e.getMessage());
            return null;
        }
    }
    
    private boolean isCacheExpired(UsageBO usageBO) {
        return usageBO.getExpireTime().isBefore(LocalDateTime.now());
    }
    
    @Async
    public void regeneratePredictionAsync(int campus, List<Double> usage) {
        String lockKey = "WaterUsage:" + campus;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(5, 300, TimeUnit.SECONDS)) {
                generateAndCachePrediction(usage, campus);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while trying to acquire lock for campus {}", campus);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
