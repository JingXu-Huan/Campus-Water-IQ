package com.ncwu.iotdevice.service.impl;

import com.ncwu.common.apis.BloomFilterService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/18
 */
@DubboService(interfaceClass = BloomFilterService.class, version = "1.0.0")
@Service
@RequiredArgsConstructor
public class BloomFilterServiceImpl implements BloomFilterService {

    private final RedissonClient redissonClient;

    @Override
    public boolean mightContains(List<String> ids) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("device:bloom");
        long contains = bloomFilter.contains(ids);
        return contains == ids.size();
    }

    @Override
    public boolean add(List<String> ids) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("device:bloom");
        return ids.size()==bloomFilter.add(ids);
    }
}
