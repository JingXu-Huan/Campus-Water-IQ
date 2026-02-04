package com.ncwu.repairservice.controller;


import com.ncwu.common.domain.vo.Result;
import com.ncwu.common.enums.ErrorCode;
import com.ncwu.repairservice.entity.UserReportDTO;
import com.ncwu.repairservice.entity.vo.UserReportVO;
import com.ncwu.repairservice.service.IDeviceReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/4
 */
@Slf4j
@RestController
@RequestMapping("/user-report")
@RequiredArgsConstructor
public class UserReportController {

    private final IDeviceReservationService deviceReservationService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 用户上报设备异常报修单
     */
    @PostMapping("/report")
    public Result<Boolean> userReport(@RequestBody @NotNull UserReportDTO userReportDTO) {
        String deviceCode = userReportDTO.getDeviceCode();
        if (!isValidDeviceId(List.of(deviceCode)))
            return Result.fail(false, ErrorCode.PARAM_VALIDATION_ERROR.code(),
                    ErrorCode.PARAM_VALIDATION_ERROR.message());
        return deviceReservationService.addAReport(userReportDTO);
    }

    /**
     * 取消报修单
     */
    @DeleteMapping("/cancel")
    public Result<Boolean> cancelReport(@RequestParam @NotNull List<String> deviceReservationId,
                                        @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
                                        @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return deviceReservationService.cancelReport(deviceReservationId);
    }

    /**
     * 批量查询一批设备编号下的所有报修记录
     */
    @PostMapping("/listByDeviceCode")
    public Result<List<UserReportVO>> listByDeviceCode(@RequestParam @NotNull List<String> deviceCode,
                                                       @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
                                                       @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        if (!isValidDeviceId(deviceCode))
            return Result.fail(Collections.emptyList(), ErrorCode.PARAM_VALIDATION_ERROR.code(),
                    ErrorCode.PARAM_VALIDATION_ERROR.message());
        return deviceReservationService.listByDeviceCode(deviceCode, pageNum, pageSize);
    }

    /**
     * 查询某台设备的所有报修记录
     */
    @GetMapping("/getByDeviceCode")
    public Result<List<UserReportVO>> getDeviceReportByDeviceCode(String deviceCode) {
        if (!isValidDeviceId(List.of(deviceCode)))
            return Result.fail(Collections.emptyList(), ErrorCode.PARAM_VALIDATION_ERROR.code(),
                    ErrorCode.PARAM_VALIDATION_ERROR.message());
        return deviceReservationService.getDeviceReportByDeviceCode(deviceCode);
    }

    /**
     * 查询当前用户的所有报修记录
     */
    @GetMapping("/listByUserId")
    public Result<List<UserReportVO>> getUserReportByUserId(String uid) {
        return deviceReservationService.getUserReportByUserName(uid);
    }

    /**
     * 查询某状态下的所有报修单
     */
    @GetMapping("/listByStatus")
    public Result<List<UserReportVO>> getDeviceReportByStatus(String status,
                                                              @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
                                                              @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return deviceReservationService.getDeviceReportByStatus(status, pageNum, pageSize);
    }

    /**
     * 检查设备ID是否合法
     */
    private boolean isValidDeviceId(List<String> deviceCode) {
        return deviceCode.stream().allMatch(code ->
                Boolean.TRUE.equals(redisTemplate.opsForSet().isMember("device:meter", code)) ||
                        Boolean.TRUE.equals(redisTemplate.opsForSet().isMember("device:sensor", code))
        );
    }
}



