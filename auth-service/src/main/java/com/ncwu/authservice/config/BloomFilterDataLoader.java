package com.ncwu.authservice.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ncwu.authservice.entity.User;
import com.ncwu.authservice.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BloomFilterDataLoader {

    private final UserMapper userMapper;
    private final List<RBloomFilter<String>> userBloomFilter;

    @Bean
    public CommandLineRunner loadBloomFilterData() {
        return args -> {
            log.info("开始加载布隆过滤器数据...");
            log.info("布隆过滤器列表大小: {}", userBloomFilter.size());
            // 检查每个布隆过滤器的状态
            userBloomFilter.forEach(bf -> {
                log.info("布隆过滤器状态: 存在={}, 预期容量={}", bf.isExists(), bf.getExpectedInsertions());
            });
            
            boolean allExists = userBloomFilter.stream().allMatch(RBloomFilter::isExists);
            if (allExists) {
                RBloomFilter<String> emailBloomFilter = userBloomFilter.getFirst();
                RBloomFilter<String> phoneBloomFilter = userBloomFilter.get(1);
                RBloomFilter<String> uidBloomFilter = userBloomFilter.get(2);
                // 清空布隆过滤器
                emailBloomFilter.delete();
                phoneBloomFilter.delete();
                uidBloomFilter.delete();
                // 重新初始化布隆过滤器
                emailBloomFilter.tryInit(100000L, 0.01);
                phoneBloomFilter.tryInit(100000L, 0.01);
                uidBloomFilter.tryInit(100000L, 0.01);
                // 查询所有有效用户（只查询一次）
                List<User> allUsers = userMapper.selectList(new LambdaQueryWrapper<User>()
                                .eq(com.ncwu.authservice.entity.User::getStatus, 1));
                log.info("找到 {} 个有效用户", allUsers.size());
                
                // 分别添加到不同的布隆过滤器
                allUsers.forEach(user -> {
                    if (user.getEmail() != null) {
                        emailBloomFilter.add(user.getEmail());
                        log.info("添加邮箱到布隆过滤器: {}", user.getEmail());
                    }
                    if (user.getPhoneNum() != null) {
                        phoneBloomFilter.add(user.getPhoneNum());
                        log.info("添加手机号到布隆过滤器: {}", user.getPhoneNum());
                    }
                    if (user.getUid() != null) {
                        uidBloomFilter.add(user.getUid());
                        log.info("添加UID到布隆过滤器: {}", user.getUid());
                    }
                });
                log.info("邮箱布隆过滤器当前元素数量: {}", emailBloomFilter.count());
                log.info("手机号布隆过滤器当前元素数量: {}", phoneBloomFilter.count());
                log.info("UID布隆过滤器当前元素数量: {}", uidBloomFilter.count());
                log.info("Redis 布隆过滤器数据加载完成");
            } else {
                log.warn("布隆过滤器未完全初始化，跳过数据加载");
            }
        };
    }
}
