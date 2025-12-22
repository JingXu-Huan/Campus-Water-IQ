package com.ncwu.iotdevice.simulator;


import lombok.Data;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/20
 */
@Data
@Component
public class WaterQualityDetector {

    private volatile boolean enable = false;

    @Scheduled(fixedRate = 1000)
    public void sendData() {
        if (!enable) {
            return;
        }
        System.out.println(Thread.currentThread().getName());
        System.out.println("上报数据...");
    }

}
