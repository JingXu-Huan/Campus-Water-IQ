package com.ncwu.iotservice.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ncwu.common.VO.Result;
import com.ncwu.iotservice.entity.IotDeviceData;
import com.ncwu.iotservice.mapper.IoTDeviceDataMapper;
import com.ncwu.iotservice.service.IoTDataService;
import org.springframework.stereotype.Service;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/20
 */
@Service
public class IoTDataServiceImpl extends ServiceImpl<IoTDeviceDataMapper, IotDeviceData> implements IoTDataService {

    @Override
    public Result<Integer> getRangeUsage(long start, long end) {
        return null;
    }
}
