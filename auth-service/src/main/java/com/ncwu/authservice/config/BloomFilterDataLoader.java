package com.ncwu.authservice.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ncwu.authservice.entity.User;
import com.ncwu.authservice.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class BloomFilterDataLoader {

    private final UserMapper userMapper;
    private final List<RBloomFilter<String>> userBloomFilter;

    @Bean
    public CommandLineRunner loadBloomFilterData() {
        return args -> {
            if (userBloomFilter.stream().allMatch(RBloomFilter::isExists)) {
                RBloomFilter<String> emailBloomFilter = userBloomFilter.getFirst();
                RBloomFilter<String> phoneBloomFilter = userBloomFilter.get(1);
                RBloomFilter<String> uidBloomFilter = userBloomFilter.get(2);
                //加载有效的邮箱
                userMapper.selectList(new LambdaQueryWrapper<User>()
                                .select(com.ncwu.authservice.entity.User::getEmail)
                                .eq(com.ncwu.authservice.entity.User::getStatus, 1))
                        .forEach(user -> emailBloomFilter.add(user.getEmail()));
                //加载有效的手机号
                userMapper.selectList(new LambdaQueryWrapper<User>()
                                .select(com.ncwu.authservice.entity.User::getPhoneNum).eq(com.ncwu.authservice.entity.User::getStatus, 1))
                        .forEach(user -> phoneBloomFilter.add(user.getPhoneNum()));
                //加载有效的uid
                userMapper.selectList(new LambdaQueryWrapper<User>().select(com.ncwu.authservice.entity.User::getUid)
                                .eq(com.ncwu.authservice.entity.User::getStatus, 1))
                        .forEach(user -> uidBloomFilter.add(user.getUid()));

                System.out.println("Redis 布隆过滤器数据加载完成");
            }
        };
    }
}
