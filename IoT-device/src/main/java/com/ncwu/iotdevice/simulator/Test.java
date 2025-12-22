package com.ncwu.iotdevice.simulator;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/22
 */
@Component
public class Test {
    @Autowired
    private WaterQualityDetector waterQualityDetector;

    public void enable() throws InterruptedException {
        Thread.sleep(1000);
        waterQualityDetector.setEnable(true);
    }
    public void disable() throws InterruptedException {
        Thread.sleep(1000);
        waterQualityDetector.setEnable(false);
    }
}
