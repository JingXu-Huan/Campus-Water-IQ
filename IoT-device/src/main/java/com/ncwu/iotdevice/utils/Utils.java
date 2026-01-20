package com.ncwu.iotdevice.utils;


import com.alibaba.nacos.shaded.com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ncwu.iotdevice.AOP.annotation.InitLuaScript;
import com.ncwu.iotdevice.config.ServerConfig;
import com.ncwu.iotdevice.domain.Bo.DeviceIdList;
import com.ncwu.iotdevice.domain.entity.VirtualDevice;
import com.ncwu.iotdevice.exception.DeviceRegisterException;
import com.ncwu.iotdevice.mapper.DeviceMapper;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ncwu.iotdevice.AOP.Aspects.InitLuaScript.Lua_script;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/21
 */
@Component
@RequiredArgsConstructor
public class Utils {
    private final StringRedisTemplate redisTemplate;

    /**
     * 此方法用于向 redis中初始化设备 id
     *
     * @param buildings     楼宇总数
     * @param floors        楼宇的层数
     * @param rooms         房间总数
     * @param redisTemplate redis 操作对象
     * @throws DeviceRegisterException 设备注册失败异常
     */
    public static DeviceIdList initAllRedisData(int buildings, int floors, int rooms,
                                                StringRedisTemplate redisTemplate,RedissonClient redissonClient) {

        if (buildings <= 0 || floors <= 0 || rooms <= 0) {
            throw new DeviceRegisterException("楼宇、楼层、房间数量必须大于 0");
        }
        Map<String, String> meterMap = new HashMap<>();
        List<String> meterDeviceIds = new ArrayList<>();
        List<String> waterQualityDeviceIds = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            for (int b = 1; b <= buildings; b++) {
                waterQualityDeviceIds.add(String.format("2%d%02d%02d001", i, b, floors));
                for (int f = 1; f <= floors; f++) {
                    for (int r = 1; r <= rooms; r++) {
                        meterDeviceIds.add(String.format("1%d%02d%02d%03d", i, b, f, r));
                    }
                }
            }
        }
        // 先删除旧的布隆过滤器，避免残留数据影响
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("device:bloom");
        bloomFilter.delete();
        bloomFilter.tryInit(100000,0.01);
        bloomFilter.add(meterDeviceIds);
        bloomFilter.add(waterQualityDeviceIds);
        Map<String, String> map = Stream.concat(meterDeviceIds.stream(), waterQualityDeviceIds.stream())
                .collect(Collectors.toMap(Function.identity(), id -> "-1"));
        try {
            //初始化标志位
            redisTemplate.opsForValue().set("isInit","1");
            //水表总用水量
            redisTemplate.opsForHash().putAll("meter:total_usage", meterMap);
            //水表设备 id 集合
            redisTemplate.opsForSet().add("device:meter", meterDeviceIds.toArray(new String[0]));
            //水质传感器设备 id 集合
            redisTemplate.opsForSet().add("device:sensor", waterQualityDeviceIds.toArray(new String[0]));
            //所以设备心跳表
            redisTemplate.opsForHash().putAll("OnLineMap", map);
            //管网发生的特殊事件
            redisTemplate.opsForValue().set("mode", "normal");
            //当天时间
            redisTemplate.opsForValue().set("Time", "0");
            //世界的季节
            redisTemplate.opsForValue().set("Season", "1");
            //水表在线状态可否受检
            redisTemplate.opsForValue().set("MeterChecked", "0");
            //水质传感器在线状态可否受检
            redisTemplate.opsForValue().set("WaterQualityChecked", "0");
        } catch (Exception e) {
            throw new DeviceRegisterException("注册失败");
        }
        return new DeviceIdList(meterDeviceIds, waterQualityDeviceIds);
    }

    /**
     * 方法清除 redis 中对应的旧数据
     */
    public static void clearRedisData(StringRedisTemplate redisTemplate, DeviceMapper deviceMapper) {
        String prefix = "device:";
        try {
            redisTemplate.opsForValue().set("isInit","0");
            redisTemplate.delete(prefix + "meter");
            redisTemplate.delete(prefix + "sensor");
            redisTemplate.delete("meter:total_usage");
            redisTemplate.delete("OnLineMap");
            redisTemplate.delete("Time");
            redisTemplate.delete("Season");
            redisTemplate.delete("WaterQualityChecked");
            redisTemplate.delete("MeterChecked");
            redisTemplate.delete(prefix + "DormitoryBuildings");
            redisTemplate.delete(prefix + "educationBuildings");
            redisTemplate.delete(prefix + "experimentBuildings");
            redisScanDel("device:OffLine:*", 1000, redisTemplate);
            deviceMapper.delete(null);
        } catch (Exception e) {
            throw new DeviceRegisterException("移除设备失败");
        }
    }

    /**
     * 方法按照字符串匹配规则删除 redis的 key
     *
     * @param pattern 匹配规则
     * @param count   扫描数量
     */
    public static void redisScanDel(String pattern, int count, StringRedisTemplate redisTemplate) {
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(count)
                .build();
        try (Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(
                connection -> connection.keyCommands().scan(options)
        )) {
            Set<String> keys = new HashSet<>();
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next()));
                // 扫到一批就删一批，避免 key 堆积
                if (keys.size() >= count) {
                    redisTemplate.delete(keys);
                    keys.clear();
                }
            }
            // 删掉最后不足一批的
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }

    public static double keep3(double v) {
        return BigDecimal.valueOf(v)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * 根据管网水流对水压进行计算和离散化
     */
    public static double waterPressureGenerate(double flow, ServerConfig serverConfig) {
        //管网初始压力
        double p0 = serverConfig.getP0();
        double pressure = p0 - 0.15 * flow + ThreadLocalRandom.current().nextDouble(0.01, 0.03);
        //离散步长
        double s = serverConfig.getStep();
        double Pdiscrete = Math.round(pressure / s) * s;
        //管网最小压力
        double Pmin = serverConfig.getPmin();
        //管网最大压力
        double Pmax = serverConfig.getPmax();
        return keep3(Math.min(Math.max(Pdiscrete, Pmin), Pmax));
    }

    /**
     * 设备上线后置处理器
     */
    public static void markDeviceOnline(String deviceCode, long timestamp, DeviceMapper deviceMapper,
                                        StringRedisTemplate redisTemplate) {
        //得到自定义线程池
        ExecutorService pools = getExecutorPools("markDeviceOnline", 5, 10, 60, 1000);
        // 1. 更新数据库状态（仅当当前为 offline 时）
        pools.submit(() -> {
            LambdaUpdateWrapper<VirtualDevice> updateWrapper =
                    new LambdaUpdateWrapper<VirtualDevice>()
                            .eq(VirtualDevice::getDeviceCode, deviceCode)
                            .eq(VirtualDevice::getStatus, "offline")
                            .set(VirtualDevice::getIsRunning, true)
                            .set(VirtualDevice::getStatus, "online");
            deviceMapper.update(updateWrapper);
        });
        // 2. 清理离线缓存
        redisTemplate.delete("device:OffLine:" + deviceCode);
        redisTemplate.delete("cache:device:status:" + deviceCode);
        // 3. 加入心跳监控
        redisTemplate.opsForHash()
                .put("OnLineMap", deviceCode, String.valueOf(timestamp));
    }


    @InitLuaScript("CheckIds.lua")
    public boolean checkId(List<String> ids) {
        Long result = redisTemplate.execute(
                Lua_script,
                List.of("device:meter"),
                ids.toArray()
        );
        return Long.valueOf(1L).equals(result);
    }

    /**
     * 得到一个自定义线程池
     *
     * @param name        线程池名称
     * @param size        核心线程池数量
     * @param maxSize     最大线程数量
     * @param seconds     线程空闲生存时间
     * @param tasksLength 最多任务数量
     * @return pool 自定义线程池
     */
    public static ExecutorService getExecutorPools(String name, int size, int maxSize, int seconds, int tasksLength) {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat(name).build();
        return new ThreadPoolExecutor(
                size,                      // 核心线程
                maxSize,                     // 最大线程
                seconds, TimeUnit.SECONDS,  // 空闲生存时间
                new ArrayBlockingQueue<>(tasksLength), // 有界队列：最多排队 1000 个
                namedThreadFactory,     // 自定义名称方便排查
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：队列满了让调用者自己执行
        );
    }

    /**
     * 方法给出检测设备上下线的一个合理时间戳
     */
    public long getNow() {
        long now = 0;
        Map<Object, Object> entries = redisTemplate.opsForHash().randomEntries("OnLineMap", 10);
        if (entries != null && !entries.isEmpty()) {
            if (entries.size() == 1) {
                return System.currentTimeMillis();
            }
            long sum = 0;
            for (Object value : entries.values()) {
                sum += Long.parseLong(value.toString());
            }
            now = sum / entries.size();  // 用实际数量除
        }
        return now;
    }
}


