package com.ncwu.iotservice.util;

import com.ncwu.iotservice.service.impl.IoTDataServiceImpl;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/13
 */
public class DataFormatUtils {
    /**
     * 将本地时区格式转化成 UTC 格式
     */
    public static IoTDataServiceImpl.DateFormatBo getDateFormatBo(LocalDateTime start, LocalDateTime end) {
        // InfluxDB Flux 要求 UTC
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        Instant startInstant = start.atZone(zoneId).toInstant();
        Instant endInstant = end.atZone(zoneId).toInstant();
        // 转换时间戳为 RFC3339
        String startTime = DateTimeFormatter.ISO_INSTANT.format(startInstant);
        String endTime = DateTimeFormatter.ISO_INSTANT.format(endInstant);
        return new IoTDataServiceImpl.DateFormatBo(startTime, endTime);
    }
}
