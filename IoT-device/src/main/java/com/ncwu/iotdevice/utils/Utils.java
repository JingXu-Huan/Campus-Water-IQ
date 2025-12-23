package com.ncwu.iotdevice.utils;


import com.ncwu.iotdevice.domain.Bo.DeviceIdList;
import com.ncwu.iotdevice.exception.DeviceRegisterException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/21
 */
public class Utils {

    public static void main(String[] args) {

    }

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

        try {
            redisTemplate.opsForHash().putAll("meterUsages", meterMap);
            redisTemplate.opsForSet().add("device:meter", meterDeviceIds.toArray(new String[0]));
            redisTemplate.opsForSet().add("device:sensor", waterQualityDeviceIds.toArray(new String[0]));
        } catch (Exception e) {
            throw new DeviceRegisterException("注册失败");
        }
        return new DeviceIdList(meterDeviceIds, waterQualityDeviceIds);
    }

    /**
     * 方法清除 redis 中对应的 set 集合
     */
    public static void clearRedisData(StringRedisTemplate redisTemplate) {
        try {
            redisTemplate.delete("device:meter");
            redisTemplate.delete("device:sensor");
            redisTemplate.delete("meterUsages");
        } catch (Exception e) {
            throw new DeviceRegisterException("移除设备失败");
        }
    }
}


