package com.ncwu.iotdevice;

import com.ncwu.iotdevice.utils.Utils;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest

class IoTDeviceApplicationTests {
    @Autowired
    private  StringRedisTemplate redisTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void initData() {
        Utils.initAllRedisData(10, 3, 5, redisTemplate);
    }

    @Test
    void clear(){
        Utils.clearRedisData(redisTemplate);
    }

}
