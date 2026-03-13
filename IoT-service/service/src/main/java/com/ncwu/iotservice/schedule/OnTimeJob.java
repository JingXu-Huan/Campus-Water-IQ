package com.ncwu.iotservice.schedule;

import com.ncwu.common.domain.vo.Result;
import com.ncwu.iotservice.config.ServiceConfig;
import com.ncwu.iotservice.entity.WaterUsageRecord;
import com.ncwu.iotservice.mapper.WaterUsageRecordMapper;
import com.ncwu.iotservice.service.IoTDataService;
import com.ncwu.iotservice.service.impl.IoTDataServiceImpl;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.ncwu.iotservice.util.DataFormatUtils.getDateFormatBo;

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
    private final ServiceConfig serviceConfig;
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 调度器，用于管理定时任务
     */
    private ScheduledExecutorService scheduler;

    double sum = 0;

    @PostConstruct
    public void init() {
        //使用虚拟线程
        scheduler = Executors.newScheduledThreadPool(10, Thread.ofVirtual().factory());
        // 启动定时任务
        run();
    }

    public void run() {
        scheduler.scheduleAtFixedRate(this::saveDailyWaterUsage, 0, serviceConfig.getSaveTimeInterval(), TimeUnit.SECONDS);
    }

    public void saveDailyWaterUsage() {
        LocalDateTime now = LocalDateTime.now();
        if (now.getHour() == 0 && now.getMinute() == 0) {
            log.info("开始执行每日用水量保存任务...");
            //todo 改用消息队列，保证消息不丢失。避免在凌晨系统宕机丢失数据。
            // rocketMQTemplate.convertAndSend("");
            for (int i = 1; i <= 3; i++) {
                saveSchoolUsage(i, sum, now);
            }
            log.info("每日用水量保存任务执行完成");
            sum = 0;
        }
        int saveTimeInterval = serviceConfig.getSaveTimeInterval();
        for (int j = 1; j <= 3; j++) {
            // 只查询最近 saveTimeInterval 分钟的数据,前一个时间段我们已经查过了
            LocalDateTime startTime = now.minusMinutes(saveTimeInterval);
            IoTDataServiceImpl.DateFormatBo dateFormatBo = getDateFormatBo(startTime, now);
            Result<Double> schoolUsage = ioTDataService
                    .getSchoolUsageFromDb(j, dateFormatBo.startTime(), dateFormatBo.endTime());
            Double data = schoolUsage.getData();
            if (data != null) {
                sum += data;
            }
        }
    }

    /**
     * 保存单个校区的用水量
     */
    private void saveSchoolUsage(Integer school, double usage, LocalDateTime now) {
        WaterUsageRecord waterUsageRecord = new WaterUsageRecord();
        waterUsageRecord.setUsage(usage);
        waterUsageRecord.setSchool(school);
        waterUsageRecord.setRecordDate(now);
        waterUsageRecordMapper.insert(waterUsageRecord);
    }
}
