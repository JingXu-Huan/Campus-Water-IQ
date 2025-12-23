package com.ncwu.iotdevice.simulator;


import cn.hutool.core.date.DateTime;
import cn.hutool.core.lang.UUID;
import com.ncwu.iotdevice.Constants.DeviceStatus;
import com.ncwu.iotdevice.domain.Bo.DeviceIdList;
import com.ncwu.iotdevice.domain.Bo.MeterDataBo;
import com.ncwu.iotdevice.domain.entity.VirtualDevice;
import com.ncwu.iotdevice.enums.SwitchModes;
import com.ncwu.iotdevice.exception.MessageSendException;
import com.ncwu.iotdevice.service.VirtualDeviceService;
import com.ncwu.iotdevice.utils.Utils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/20
 */
@Slf4j
@Data
@Component
@RequiredArgsConstructor
public class WaterMeter {

    private volatile SwitchModes currentMode = SwitchModes.NORMAL; // 默认正常模拟

    private final StringRedisTemplate redisTemplate;
    private final VirtualDeviceService virtualDeviceService;

    /**
     * 方法用于向数据库和 redis 中注册设备
     *
     * @param buildings 楼宇数量
     * @param floors    楼层数量
     * @param rooms     房间数量
     *
     */
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
        executor.submit(() ->virtualDeviceService.saveBatch(metervirtualDeviceList, 2000));
        executor.submit(() -> virtualDeviceService.saveBatch(waterQualityvirtualDeviceList, 2000));
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);
    }
    /**
     * 此方法产生正常模式的模拟数据
     *
     */
    //todo 这里时间也要支持热部署，即接收到参数之后，不重启应用生效。
    @Scheduled(fixedRate = 3000)
    public void normalGenerateMeterData() {
        if (currentMode != SwitchModes.NORMAL) {
            return;
        }
        Set<String> ids = redisTemplate.opsForSet().members("device:meter");
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Random random = new Random();
        List<MeterDataBo> dataBoList = new ArrayList<>(ids.size());
        /*todo 这里的水温，流量，瞬时流量应当受季节，天气，时间等影响
         * todo 由于定时任务不接受参数，应当从 redis获取 。
         */
        ids.forEach(id -> {
            MeterDataBo dataBo = new MeterDataBo();
            dataBo.setDevice(1);
            dataBo.setDeviceId(id);
            dataBo.setTimeStamp(LocalDateTime.now());
            double flow = random.nextDouble(0.05, 0.3);
            dataBo.setFlow(flow);// L/s

            //todo 这里总流量需要去设计redis数据结构,拿到上一次的再加上本次模拟的数据
            //todo 暂时先写死
            dataBo.setTotalUsage(1000.0);

            double pressure = random.nextDouble(0.2, 0.35);
            dataBo.setPressure(pressure);
            double waterTem = random.nextDouble(20, 37.5);
            dataBo.setWaterTem(waterTem);
            dataBo.setIsOpen(DeviceStatus.NORMAL);
            dataBo.setStatus(DeviceStatus.NORMAL);
            dataBoList.add(dataBo);
        });
        try {
            sendData(dataBoList);
            log.debug("上报数据成功");
        } catch (MessageSendException e) {
            throw new MessageSendException("数据发送失败");
        }
    }

    /**
     * 方法用于向消息队列中发送水表模拟数据
     *
     * @param dataBoList 要发送的数据集合
     * @throws MessageSendException 消息发送失败异常
     */
    private void sendData(List<MeterDataBo> dataBoList) throws MessageSendException {

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
