package com.ncwu.iotdevice.simulator;


import cn.hutool.core.date.DateTime;
import cn.hutool.core.lang.UUID;
import com.ncwu.iotdevice.domain.Bo.DeviceIdList;
import com.ncwu.iotdevice.domain.entity.VirtualDevice;
import com.ncwu.iotdevice.service.VirtualDeviceService;
import com.ncwu.iotdevice.utils.Utils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/20
 */
@Component
@RequiredArgsConstructor
public class WaterMeter {

    private final StringRedisTemplate redisTemplate;
    private final VirtualDeviceService virtualDeviceService;

    public void init(int buildings, int floors, int rooms) throws InterruptedException {
        Utils.clearRedisData(redisTemplate);
        DeviceIdList deviceIdList = Utils.initAllRedisData(buildings, floors, rooms, redisTemplate);
        List<String> meterDeviceIds = deviceIdList.getMeterDeviceIds();
        List<String> waterQualityDeviceIds = deviceIdList.getWaterQualityDeviceIds();
        List<VirtualDevice> metervirtualDeviceList = new ArrayList<>();
        List<VirtualDevice> waterQualityvirtualDeviceList = new ArrayList<>();
        build(meterDeviceIds, metervirtualDeviceList, 1);
        build(waterQualityDeviceIds, waterQualityvirtualDeviceList, 2);
        //线程池跑
        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() ->
                virtualDeviceService.saveBatch(metervirtualDeviceList, 2000)
        );
        executor.submit(() ->
                virtualDeviceService.saveBatch(waterQualityvirtualDeviceList, 2000)
        );
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);
    }

    private static void build(List<String> deviceIds, List<VirtualDevice> virtualDeviceList, int type) {
        Date now = DateTime.now();
        deviceIds.forEach(id -> {
            VirtualDevice virtualDevice = new VirtualDevice();
            virtualDevice.setDeviceType(type);
            // deviceCode 格式是 ABCXYZZZ
            virtualDevice.setDeviceCode(id);               // 设置业务唯一编码
            String sn = "JX" + UUID.fastUUID().toString().substring(0, 18);
            virtualDevice.setInstallDate(now);
            virtualDevice.setSnCode(sn);
            virtualDevice.setBuildingNo(id.substring(1, 3)); // BC
            virtualDevice.setFloorNo(id.substring(3, 5));    // XY 两位字符串
            virtualDevice.setRoomNo(id.substring(5, 8));     // ZZZ 三位房间号
            virtualDeviceList.add(virtualDevice);
        });
    }

}
