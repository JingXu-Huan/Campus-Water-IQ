package com.ncwu.repairservice.controller;


import com.ncwu.common.domain.vo.Result;
import com.ncwu.common.enums.ErrorCode;
import com.ncwu.repairservice.entity.dto.UserReportDTO;
import com.ncwu.repairservice.entity.vo.UserReportVO;
import com.ncwu.repairservice.service.IDeviceReservationService;
import com.ncwu.repairservice.tools.Utils;
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
        if (Utils.isUnValidDeviceId(List.of(deviceCode), redisTemplate))
            return Result.fail(false, ErrorCode.PARAM_VALIDATION_ERROR.code(),
                    ErrorCode.PARAM_VALIDATION_ERROR.message());
        return deviceReservationService.addAReport(userReportDTO);
    }

    /**
     * 用户取消自己的报修单
     */
    @DeleteMapping("/cancel")
    public Result<Boolean> cancelReport(@RequestParam @NotNull List<String> deviceReservationId) {
        return deviceReservationService.cancelReport(deviceReservationId);
    }

    /**
     * 用户修改报修单的状态
     */
    @GetMapping("/changeStatus")
    public Result<Boolean> changeStatus(@Pattern(regexp = "DRAFT|CONFIRMED|CANCELLED",
                                                message = "status 只能是 " + "DRAFT、CONFIRMED、CANCELLED") String status,
                                        String deviceReservationId) {
        return deviceReservationService.changeStatus(status, deviceReservationId);
    }

    /**
     * 用户查询自己的设备的所有报修记录
     */
    @GetMapping("/getByUid")
    public Result<List<UserReportVO>> getDeviceReportByUid(String uid, int pageNum, int pageSize) {

        if (isUnValidDeviceId(List.of(uid), redisTemplate))
            return Result.fail(Collections.emptyList(), ErrorCode.PARAM_VALIDATION_ERROR.code(),
                    ErrorCode.PARAM_VALIDATION_ERROR.message());
        return deviceReservationService.getDeviceReportByUid(uid, pageNum, pageSize);
    }
}



