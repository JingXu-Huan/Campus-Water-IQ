package com.ncwu.common.apis.repair_service;


import com.ncwu.common.domain.dto.UserReportDTO;
import com.ncwu.common.domain.vo.Result;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/13
 */
public interface DeviceReservationServiceApi {
    Result<Boolean> addAReport(UserReportDTO userReportDTO);
}
