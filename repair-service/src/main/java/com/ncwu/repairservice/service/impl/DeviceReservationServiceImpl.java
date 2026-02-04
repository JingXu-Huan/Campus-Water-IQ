package com.ncwu.repairservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ncwu.common.domain.vo.Result;
import com.ncwu.repairservice.entity.UserReportDTO;
import com.ncwu.repairservice.entity.po.DeviceReservation;
import com.ncwu.repairservice.entity.vo.UserReportVO;
import com.ncwu.repairservice.mapper.DeviceReservationMapper;
import com.ncwu.repairservice.service.IDeviceReservationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 智能报修/预约表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-01-15
 */
@Service
public class DeviceReservationServiceImpl extends ServiceImpl<DeviceReservationMapper, DeviceReservation> implements IDeviceReservationService {
    private final Object lock = new Object();

    @Override
    public Result<Boolean> addAReport(UserReportDTO userReportDTO) {
        String deviceCode = userReportDTO.getDeviceCode();
        int type = Integer.parseInt(deviceCode.substring(0, 1));
        DeviceReservation deviceReservation = getDeviceReservation(userReportDTO, deviceCode, type);
        LambdaQueryChainWrapper<DeviceReservation> eq = this.lambdaQuery()
                .eq(DeviceReservation::getReporterName, userReportDTO.getReportName())
                .eq(DeviceReservation::getDeviceCode, userReportDTO.getDeviceCode())
                .eq(DeviceReservation::getStatus, userReportDTO.getStatus())
                .eq(DeviceReservation::getFaultDesc, userReportDTO.getDesc());
        //todo 使用分布式锁
        synchronized (lock) {
            if (this.exists(eq)) {
                return Result.fail(false, "当前报修单已存在");
            } else save(deviceReservation);
        }
        return Result.ok(true);
    }

    private static @NonNull DeviceReservation getDeviceReservation(UserReportDTO userReportDTO, String deviceCode, int type) {
        int campusNo = Integer.parseInt(deviceCode.substring(1, 2));
        int buildingNo = Integer.parseInt(deviceCode.substring(2, 4));
        int floorNo = Integer.parseInt(deviceCode.substring(4, 6));
        int unitNo = Integer.parseInt(deviceCode.substring(6, 9));
        DeviceReservation deviceReservation = new DeviceReservation();
        deviceReservation.setDeviceType(type);
        deviceReservation.setDeviceCode(deviceCode);
        deviceReservation.setCampusNo(campusNo);
        deviceReservation.setBuildingNo(String.valueOf(buildingNo));
        deviceReservation.setFloorNo(String.valueOf(floorNo));
        deviceReservation.setUnitNo(String.valueOf(unitNo));
        deviceReservation.setReporterName(userReportDTO.getReportName());
        deviceReservation.setContactInfo(userReportDTO.getContactInfo());
        deviceReservation.setFaultDesc(userReportDTO.getDesc());
        deviceReservation.setSeverity(userReportDTO.getSeverity());
        deviceReservation.setStatus(userReportDTO.getStatus());
        deviceReservation.setRemark(userReportDTO.getRemark());
        return deviceReservation;
    }

    // 分页查询示例
    @Override
    public Result<List<UserReportVO>> getDeviceReportByStatus(String status, int pageNum, int pageSize) {
        Page<DeviceReservation> page = new Page<>(pageNum, pageSize);
        IPage<DeviceReservation> result = this.lambdaQuery()
                .eq(DeviceReservation::getStatus, status)
                .page(page);
        List<UserReportVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return Result.ok(voList);
    }

    @Override
    public Result<List<UserReportVO>> getUserReportByUserName(String userName) {
        LambdaQueryWrapper<DeviceReservation> eq = new LambdaQueryWrapper<DeviceReservation>()
                .eq(DeviceReservation::getReporterName, userName);
        List<UserReportVO> list = this.list(eq).stream().map(this::toVO).toList();
        return Result.ok(list);
    }

    @Override
    public Result<Boolean> cancelReport(List<String> deviceReservationId) {
        LambdaUpdateWrapper<DeviceReservation> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(DeviceReservation::getId, deviceReservationId);
        wrapper.set(DeviceReservation::getStatus, "CANCELLED");
        boolean update = update(wrapper);
        if (update) {
            return Result.ok(true);
        } else return Result.fail(false);
    }

    @Override
    public Result<List<UserReportVO>> listByDeviceCode(List<String> deviceCode, int pageNum, int pageSize) {
        Page<DeviceReservation> page = new Page<>(pageNum, pageSize);
        IPage<DeviceReservation> result = this.lambdaQuery()
                .eq(DeviceReservation::getDeviceCode, deviceCode)
                .page(page);
        List<UserReportVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return Result.ok(voList);
    }

    @Override
    public Result<List<UserReportVO>> getDeviceReportByDeviceCode(String deviceCode) {
        LambdaQueryWrapper<DeviceReservation> eq = new LambdaQueryWrapper<DeviceReservation>()
                .eq(DeviceReservation::getDeviceCode, deviceCode);
        List<UserReportVO> list = this.list(eq).stream().map(this::toVO).toList();
        return Result.ok(list);
    }

    private UserReportVO toVO(DeviceReservation deviceReservation) {
        UserReportVO vo = new UserReportVO();
        vo.setId(deviceReservation.getId().toString());
        vo.setDeviceCode(deviceReservation.getDeviceCode());
        vo.setReportName(deviceReservation.getReporterName());
        vo.setContactInfo(deviceReservation.getContactInfo());
        vo.setDesc(deviceReservation.getFaultDesc());
        vo.setSeverity(deviceReservation.getSeverity());
        vo.setStatus(deviceReservation.getStatus());
        vo.setRemark(deviceReservation.getRemark());
        vo.setCreatedAt(deviceReservation.getCreatedAt());
        vo.setUpdatedAt(deviceReservation.getUpdatedAt());
        return vo;
    }
}


