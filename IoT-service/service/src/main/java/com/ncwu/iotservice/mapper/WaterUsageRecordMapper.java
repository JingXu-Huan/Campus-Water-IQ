package com.ncwu.iotservice.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncwu.iotservice.entity.VO.UsageBO;
import com.ncwu.iotservice.entity.WaterUsageRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/8
 */
@Mapper
public interface WaterUsageRecordMapper extends BaseMapper<WaterUsageRecord> {

    /**
     * 查询某校区近N天的用水记录
     */
    @Select("SELECT * FROM water_usage_record WHERE school = #{school} AND record_date >= #{startDate} ORDER BY record_date DESC")
    List<WaterUsageRecord> selectRecentRecords(@Param("school") Integer school, @Param("startDate") LocalDateTime startDate);

    /**
     * 查询近七天的水量数据
     */
    @Select("""
            select school as campus, water_usage_record.usage as schoolUsage, water_usage_record.record_date as `time`
            from water_usage_record
            where school = #{campus}
              and record_date >= CURDATE() - interval 7 day
            order by record_date""")
    List<UsageBO> waterTrendsForTheWeek(int campus);
}
