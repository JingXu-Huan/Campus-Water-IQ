package com.ncwu.iotdevice.utils;


import com.ncwu.iotdevice.domain.Bo.DeviceIdList;
import com.ncwu.iotdevice.exception.DeviceRegisterException;
import com.ncwu.iotdevice.mapper.DeviceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/21
 */
@Component
@RequiredArgsConstructor
public class Utils {


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
                                                StringRedisTemplate redisTemplate) {

        if (buildings <= 0 || floors <= 0 || rooms <= 0) {
            throw new DeviceRegisterException("楼宇、楼层、房间数量必须大于 0");
        }
        Map<String, String> meterMap = new HashMap<>();
        List<String> meterDeviceIds = new ArrayList<>();
        List<String> waterQualityDeviceIds = new ArrayList<>();
        for (int b = 1; b <= buildings; b++) {
            // 每栋楼 1 台水质传感器
            String sensorId = String.format("2%02d00001", b);
            waterQualityDeviceIds.add(sensorId);
            for (int f = 1; f <= floors; f++) {
                for (int r = 1; r <= rooms; r++) {
                    String meterId = String.format("1%02d%02d%03d", b, f, r);
                    meterMap.put(meterId, "0");
                    meterDeviceIds.add(meterId);
                }
            }
        }
        Map<String, String> map = Stream.concat(meterDeviceIds.stream(), waterQualityDeviceIds.stream())
                .collect(Collectors.toMap(Function.identity(), id -> "-1"));
        try {
            redisTemplate.opsForHash().putAll("meter:total_usage", meterMap);
            redisTemplate.opsForSet().add("device:meter", meterDeviceIds.toArray(new String[0]));
            redisTemplate.opsForSet().add("device:sensor", waterQualityDeviceIds.toArray(new String[0]));
            redisTemplate.opsForHash().putAll("OnLineMap",map);
        } catch (Exception e) {
            throw new DeviceRegisterException("注册失败");
        }
        return new DeviceIdList(meterDeviceIds, waterQualityDeviceIds);
    }

    /**
     * 方法清除 redis 中对应的 set 集合
     */
    public static void clearRedisData(StringRedisTemplate redisTemplate, DeviceMapper deviceMapper) {
        try {
            redisTemplate.delete("device:meter");
            redisTemplate.delete("device:sensor");
            redisTemplate.delete("meter:total_usage");
            redisTemplate.delete("OnLineMap");
            deviceMapper.delete(null);
        } catch (Exception e) {
            throw new DeviceRegisterException("移除设备失败");
        }
    }

    /**
     * 方法按照字符串匹配规则删除 redis的 key
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
}


