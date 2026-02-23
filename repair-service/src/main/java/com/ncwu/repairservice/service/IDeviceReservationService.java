package com.ncwu.repairservice.service;

import com.ncwu.common.domain.vo.Result;
import com.ncwu.repairservice.entity.dto.UserReportDTO;
import com.ncwu.repairservice.entity.po.DeviceReservation;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ncwu.repairservice.entity.vo.UserReportVO;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 智能报修/预约表 服务类
 * </p>
 *
 * @author author
 * @since 2026-01-15
 */
public interface IDeviceReservationService extends IService<DeviceReservation> {

    Result<Boolean> addAReport(UserReportDTO userReportDTO);

    Result<List<UserReportVO>> getDeviceReportByStatus(String status, int pageNum, int pageSize);

    Result<List<UserReportVO>> getUserReportByUserName(String userName);

    Result<Boolean> cancelReport(List<String> deviceReservationId);

    Result<List<UserReportVO>> listByDeviceCode(Collection<String> deviceCode, int pageNum, int pageSize);

    Result<List<UserReportVO>> getDeviceReportByDeviceCode(String deviceCode);

    Result<Boolean> changeStatus(String status, String deviceReservationId);

    Result<List<UserReportVO>> getDeviceReportByUid(String uid, int pageNum, int pageSize);

    Result<Long> getAllUnClosedNums();
}
