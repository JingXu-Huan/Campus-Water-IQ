package com.ncwu.repairservice.controller;


import com.ncwu.common.domain.vo.Result;
import com.ncwu.common.enums.ErrorCode;
import com.ncwu.repairservice.entity.vo.UserReportVO;
import com.ncwu.repairservice.service.IDeviceReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Collections;
import java.util.List;

import static com.ncwu.repairservice.tools.Utils.isUnValidDeviceId;

/**
 * 管理/运维控制层
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/6
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/operations")
public class OperationsController {
    private final StringRedisTemplate redisTemplate;
    private final IDeviceReservationService deviceReservationService;

    /**
     * 查询某状态下的所有报修单
     */
    @GetMapping("/listByStatus")
    public Result<List<UserReportVO>> getDeviceReportByStatus(
            @Pattern(regexp = "DRAFT|CONFIRMED|PROCESSING|DONE|CANCELLED", message = "status 只能是 " +
                    "DRAFT、CONFIRMED、PROCESSING、DONE 或 CANCELLED") String status,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return deviceReservationService.getDeviceReportByStatus(status, pageNum, pageSize);
    }

    /**
     * 修改报修单状态
     */
    @GetMapping("/changeStatus")
    public Result<Boolean> changeStatus(@Pattern(regexp = "DRAFT|CONFIRMED|PROCESSING|DONE|CANCELLED",
                                                message = "status 只能是 " + "DRAFT、CONFIRMED、PROCESSING、DONE 或 CANCELLED") String status,
                                        String deviceReservationId) {
        return deviceReservationService.changeStatus(status, deviceReservationId);
    }

    /**
     * 查询某报修人的所有报修记录
     */
    @GetMapping("/listByUserId")
    public Result<List<UserReportVO>> getUserReportByUserId(String userName) {
        return deviceReservationService.getUserReportByUserName(userName);
    }

    /**
     * 查询某台设备的所有报修记录
     */
    @GetMapping("/getByDeviceCode")
    public Result<List<UserReportVO>> getDeviceReportByDeviceCode(String deviceCode) {
        if (isUnValidDeviceId(List.of(deviceCode),redisTemplate))
            return Result.fail(Collections.emptyList(), ErrorCode.PARAM_VALIDATION_ERROR.code(),
                    ErrorCode.PARAM_VALIDATION_ERROR.message());
        return deviceReservationService.getDeviceReportByDeviceCode(deviceCode);
    }
    /**
     * 批量查询一批设备编号下的所有报修记录
     */
    @PostMapping("/listByDeviceCode")
    public Result<List<UserReportVO>> listByDeviceCode(@RequestParam @NotNull List<String> deviceCode,
                                                       @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
                                                       @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        if (isUnValidDeviceId(deviceCode,redisTemplate))
            return Result.fail(Collections.emptyList(), ErrorCode.PARAM_VALIDATION_ERROR.code(),
                    ErrorCode.PARAM_VALIDATION_ERROR.message());
        return deviceReservationService.listByDeviceCode(deviceCode, pageNum, pageSize);
    }

}
