package com.ncwu.iotdevice;

import com.ncwu.iotdevice.utils.Utils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.ThreadLocalRandom;

class IoTDeviceApplicationTests {
//    @Autowired
//    private StringRedisTemplate redisTemplate;


    @Test
    void clear() {
//        Utils.clearRedisData(redisTemplate);
    }

//
//    @Test
//    void redisTest() {
//        for (int i = 0; i < 10; i++) {
//            redisTemplate.opsForValue().set("jingxu:" + i, String.valueOf(i));
//        }
//
//    }

    @Test
    void waterPressureGenerate() {
        //管网初始压力
        double p0 = 0.4;
        double pressure = p0 - 0.15 * 0 + ThreadLocalRandom.current().nextDouble(0.01, 0.03);
        //离散步长
        double s = 0.01;
        double Pdiscrete = Math.round(pressure / s) * s;
        //管网最小压力
        double Pmin = 0.12;
        //管网最大压力
        double Pmax = 0.45;
        System.out.println(Math.min(Math.max(Pdiscrete, Pmin), Pmax));
    }
}
