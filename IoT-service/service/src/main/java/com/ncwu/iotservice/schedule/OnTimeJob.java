package com.ncwu.iotservice.schedule;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ncwu.common.domain.vo.Result;
import com.ncwu.iotservice.entity.WaterUsageRecord;
import com.ncwu.iotservice.mapper.WaterUsageRecordMapper;
import com.ncwu.iotservice.service.IoTDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 每天24点准时调用方法，保存当天用水量到数据库
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/8
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnTimeJob {
    private final WaterUsageRecordMapper waterUsageRecordMapper;
    private final IoTDataService ioTDataService;

    /**
     * 校区列表
     */
    private static final List<Integer> SCHOOLS = List.of(1, 2, 3);

    /**
     * 每天 0 点执行，保存昨日用水量到数据库
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void saveDailyWaterUsage() {
        log.info("开始执行每日用水量保存任务...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterdayStart = now.minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime yesterdayEnd = now.minusDays(1).withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        for (Integer school : SCHOOLS) {
            try {
                saveSchoolUsage(school, yesterdayStart, yesterdayEnd);
            } catch (Exception e) {
                log.error("保存校区 {} 用水量失败: {}", school, e.getMessage(), e);
            }
        }

        log.info("每日用水量保存任务执行完成");
    }

    /**
     * 保存单个校区的用水量
     */
    private void saveSchoolUsage(Integer school, LocalDateTime start, LocalDateTime end) {
        // 检查是否已存在记录
        if (hasRecordForDate(school, start.toLocalDate())) {
            log.info("校区 {} 日期 {} 已存在记录，跳过", school, start.toLocalDate());
            return;
        }

        // 获取用水量
        Result<Double> result = ioTDataService.getSchoolUsage(school, start, end);

        if (result != null && "200".equals(result.getCode()) && result.getData() != null) {
            Double usage = result.getData();

            WaterUsageRecord record = new WaterUsageRecord();
            record.setSchool(school);
            record.setUsage(usage);
            waterUsageRecordMapper.insert(record);
            log.info("校区 {} 用水量保存成功: {} m³, 日期: {}", school, usage, start.toLocalDate());
        } else {
            log.warn("获取校区 {} 用水量失败, result: {}", school, result);
        }
    }

    /**
     * 检查指定日期是否已有记录
     */
    private boolean hasRecordForDate(Integer school, java.time.LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        LambdaQueryWrapper<WaterUsageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WaterUsageRecord::getSchool, school)
                .ge(WaterUsageRecord::getRecordDate, startOfDay)
                .le(WaterUsageRecord::getRecordDate, endOfDay);

        return waterUsageRecordMapper.selectCount(wrapper) > 0;
    }
}
