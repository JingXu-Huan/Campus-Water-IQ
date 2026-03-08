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

    @Override
    public Result<String> suggestionOfWaterUsage() {
        String suggestion = redisTemplate.opsForValue().get("suggestion");
        if (suggestion == null) {
            RLock lock = redissonClient.getLock("suggestions");
            String response;
            try {
                if (lock.tryLock()) {
                    response = chatLanguageModel
                            .chat("请使用中文生成一个20字左右的节水建议。Example response: 刷牙的时候记得把水龙头关掉哦");
                    redisTemplate.opsForValue().set("suggestion", response, 30, TimeUnit.MINUTES);
                } else return Result.ok("刷牙的时候记得把水龙头关掉哦");
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
            return Result.ok(response);
        } else return Result.ok(suggestion);
    }

    @Override
    public Result<String> suggestionOfWater(int score, double ph, double ch, double th) {
        String cacheKey = "water_suggestion";
        String lockKey = "lock:" + cacheKey;

        // 先尝试从缓存获取
        String cachedResult = redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
            return Result.ok(cachedResult);
        }

        // 获取分布式锁
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                try {
                    // 双重检查，防止其他线程已经设置了缓存
                    cachedResult = redisTemplate.opsForValue().get(cacheKey);
                    if (cachedResult != null) {
                        return Result.ok(cachedResult);
                    }
                    // 调用AI API
                    String res = chatLanguageModel
                            .chat("请根据我给你提供的水质信息，作出评价并且给出建议(50字)" +
                                    "：分数：" + score + "ph" + ph + "浊度" + th + "含氯量" + ch);
                    // 缓存结果，设置10分钟过期
                    redisTemplate.opsForValue().set(cacheKey, res, 10, TimeUnit.MINUTES);
                    return Result.ok(res);
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                // 获取锁失败，返回默认建议或错误信息
                return Result.fail(null, "系统繁忙，请稍后再试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁时被中断: {}", e.getMessage());
            return Result.fail(null, "系统错误，请稍后再试");
        } catch (Exception e) {
            log.error("水质建议生成失败: {}", e.getMessage());
            return Result.fail(null, "服务暂时不可用，请稍后再试");
        }
    }

    private double getRes(List<Double> usage) {
        try {
            String response = chatLanguageModel.chat(
                    "Predict the next water usage value based on this data: " + usage.toString() +
                            ". Return ONLY a single number without any explanation, text, or formatting. " +
                            "Example response: 209.25"
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
        UsageBO usageBO = new UsageBO(predictedValue, LocalDateTime.now().plusMinutes(5));

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
