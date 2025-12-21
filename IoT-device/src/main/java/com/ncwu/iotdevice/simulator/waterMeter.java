package com.ncwu.iotdevice.simulator;


import com.ncwu.iotdevice.Bo.DeviceIdList;
import com.ncwu.iotdevice.utils.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/20
 */
@Component
@RequiredArgsConstructor
public class waterMeter {
    private final StringRedisTemplate redisTemplate;

    public void init() {
        DeviceIdList deviceIdList = Utils.initAllRedisData(5, 3, 3, redisTemplate);
        //todo 数据库
        List<String> meterDeviceIds = deviceIdList.getMeterDeviceIds();
        List<String> waterQualityDeviceIds = deviceIdList.getWaterQualityDeviceIds();

    }
}
