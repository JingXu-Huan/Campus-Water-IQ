package com.ncwu.common.apis.iot_service;

import com.ncwu.common.domain.bo.ToAIBO;
import com.ncwu.common.domain.vo.Result;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/10
 */
public interface IotDataService {

    /**
     * 获取近七天的用水历史数据（三个校区）
     */
    Result<ToAIBO> getRecentWeekUsage();

    /**
     * 水质合格率
     */
    Result<Double> getQualityRate();


    /**
     * 获得所有设备的离线率
     */
    Result<Double> getOfflineRate();

    /**
     * 获得校区用水波动
     */
    Result<Map<String, Double>> getSwings();

    /**
     * 得到某校区某段时间的用水量
     */
    Result<Double> getSchoolUsage(int school, LocalDateTime start, LocalDateTime end);

    //todo 描述tools=========================================================================================================

    /**
     * 得到水质评分
     */
    Result<Double> getWaterQualityScore(String deviceId);

    /**
     * 得到设备健康度
     */
    Result<Double> getHealthyScoreOfDevices();

    /**
     * 得到某校区下线设备集合
     */
    Result<Collection<String>> getOffLineList(int campus);

    /**
     * 得到某个区域在校园场景的用水比例
     */
    Result<Double> getRate(int region, int campus);

    /**
     * 获取设备上报原始数据报表
     */
    ResponseEntity<byte[]> getDeviceDatas(String deviceCode);
}
