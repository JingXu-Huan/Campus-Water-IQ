package com.ncwu.common.apis;

import com.ncwu.common.domain.bo.ToAIBO;
import com.ncwu.common.domain.vo.Result;

/**
 * IoT-service Dubbo 接口
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/8
 */
public interface IoTDataServiceApi {

    /**
     * 获取近七天的用水历史数据（三个校区）
     */
    Result<ToAIBO> getRecentWeekUsage();
}
