package com.ncwu.common.apis.iot_service;

import com.ncwu.common.domain.bo.ToAIBO;
import com.ncwu.common.domain.vo.Result;

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
}
