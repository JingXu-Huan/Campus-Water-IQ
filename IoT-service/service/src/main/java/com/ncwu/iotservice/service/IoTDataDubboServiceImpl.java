package com.ncwu.iotservice.service;

import com.ncwu.common.apis.IoTDataServiceApi;
import com.ncwu.common.domain.bo.ToAIBO;
import com.ncwu.common.domain.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * IoT-service Dubbo 接口实现
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/8
 */
@Slf4j
@DubboService(version = "1.0.0", interfaceClass = IoTDataServiceApi.class)
@Service
@RequiredArgsConstructor
public class IoTDataDubboServiceImpl implements IoTDataServiceApi {

    private final com.ncwu.iotservice.mapper.WaterUsageRecordMapper waterUsageRecordMapper;

    @Override
    public Result<ToAIBO> getRecentWeekUsage() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        
        List<Double> HY = new ArrayList<>();
        List<Double> LH = new ArrayList<>();
        List<Double> JH = new ArrayList<>();
        
        // 查询三个校区的数据
        for (int school = 1; school <= 3; school++) {
            List<com.ncwu.iotservice.entity.WaterUsageRecord> records = 
                    waterUsageRecordMapper.selectRecentRecords(school, startDate);
            List<Double> usageList = records.stream()
                    .map(com.ncwu.iotservice.entity.WaterUsageRecord::getUsage)
                    .collect(Collectors.toList());
            
            if (school == 1) {
                HY = usageList;
            } else if (school == 2) {
                LH = usageList;
            } else {
                JH = usageList;
            }
        }
        
        ToAIBO toAIBO = new ToAIBO();
        toAIBO.setHY(HY);
        toAIBO.setLH(LH);
        toAIBO.setJH(JH);
        return Result.ok(toAIBO);
    }
}
