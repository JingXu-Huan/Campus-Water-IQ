package com.ncwu.repairservice.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ncwu.common.domain.vo.Result;
import com.ncwu.repairservice.entity.UserReportDTO;
import com.ncwu.repairservice.entity.po.DeviceReservation;
import com.ncwu.repairservice.entity.vo.UserReportVO;
import com.ncwu.repairservice.mapper.DeviceReservationMapper;
import com.ncwu.repairservice.service.IDeviceReservationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.ncwu.common.utils.Utils.getExecutorPools;

/**
 * <p>
 * 智能报修/预约表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-01-11
 */
@Service
@RequiredArgsConstructor
public class DeviceReservationServiceImpl extends ServiceImpl<DeviceReservationMapper, DeviceReservation> implements IDeviceReservationService {

    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;
    private ExecutorService pool;

    @PostConstruct
    public void init() {
        pool = getExecutorPools("userReport-work-Thread", 1, 10, 120, 1000);
    }

    @Override
    public Result<Boolean> addAReport(UserReportDTO userReportDTO) {
        String deviceCode = userReportDTO.getDeviceCode();
        int type = Integer.parseInt(deviceCode.substring(0, 1));
        DeviceReservation deviceReservation = getDeviceReservation(userReportDTO, deviceCode, type);
        LambdaQueryWrapper<DeviceReservation> eq = new LambdaQueryWrapper<DeviceReservation>()
                .eq(DeviceReservation::getReporterName, userReportDTO.getReportName())
                .eq(DeviceReservation::getDeviceCode, userReportDTO.getDeviceCode())
                .eq(DeviceReservation::getStatus, userReportDTO.getStatus())
                .eq(DeviceReservation::getFaultDesc, userReportDTO.getDesc());
        //todo 将1L替换成用户ID
        RLock lock = redissonClient.getLock("user:add:reservation" + 1L);
        lock.lock();
        try {
            if (this.exists(eq)) {
                return Result.fail(false, "当前报修单已存在");
            } else save(deviceReservation);
        } finally {
            lock.unlock();
        }
        return Result.ok(true);
    }

    private static DeviceReservation getDeviceReservation(UserReportDTO userReportDTO, String deviceCode, int type) {
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
        //尝试查询缓存
        String s = redisTemplate.opsForValue().get("ReportByStatus:" + status);
        //反序列化回源对象
        if (s != null) {
            return Result.ok(JSON.parseArray(s, UserReportVO.class).stream().toList());
        }
        //尝试查询数据库
        else {
            Page<DeviceReservation> page = new Page<>(pageNum, pageSize);
            IPage<DeviceReservation> result = this.lambdaQuery()
                    .eq(DeviceReservation::getStatus, status)
                    .page(page);
            List<UserReportVO> voList = result.getRecords().stream()
                    .map(this::toVO)
                    .toList();
            //异步写入 redis
            pool.submit(() -> {
                redisTemplate.opsForValue().set("ReportByStatus:" + status, JSON.toJSONString(voList),
                        120 + ThreadLocalRandom.current().nextInt(30), TimeUnit.SECONDS);
            });
            return Result.ok(voList);
        }
    }

    @Override
    public Result<List<UserReportVO>> getUserReportByUserName(String userName) {
        //查询 redis
        String s = redisTemplate.opsForValue().get("UserReportByUserName:" + userName);
        if (s != null) {
            if (s.isEmpty()) {
                return Result.ok(Collections.emptyList());
            }
            return Result.ok(JSON.parseArray(s, UserReportVO.class).stream().toList());
        }
        else {
            LambdaQueryWrapper<DeviceReservation> eq = new LambdaQueryWrapper<DeviceReservation>()
                    .eq(DeviceReservation::getReporterName, userName);
            List<UserReportVO> list = this.list(eq).stream().map(this::toVO).toList();
            if (list.isEmpty()) {
                // 缓存空结果防止缓存穿透
                redisTemplate.opsForValue().set("UserReportByUserName:" + userName, "",
                        10 + ThreadLocalRandom.current().nextInt(1), TimeUnit.MINUTES);
                return Result.ok(Collections.emptyList());
            }
            pool.submit(() -> redisTemplate.opsForValue()
                    .set("UserReportByUserName:" + userName, JSON.toJSONString(list),
                            120 + ThreadLocalRandom.current().nextInt(30), TimeUnit.SECONDS));
            return Result.ok(list);
        }
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
        //从redis查询
        String s = redisTemplate.opsForValue().get("DeviceReportByDeviceCode:" + deviceCode);
        if (s != null) {
            return Result.ok(JSON.parseArray(s, UserReportVO.class).stream().toList());
        } else {
            Page<DeviceReservation> page = new Page<>(pageNum, pageSize);
            IPage<DeviceReservation> result = this.lambdaQuery()
                    .eq(DeviceReservation::getDeviceCode, deviceCode)
                    .page(page);
            List<UserReportVO> voList = result.getRecords().stream()
                    .map(this::toVO)
                    .collect(Collectors.toList());
            //写入redis
            pool.submit(() -> redisTemplate.opsForValue()
                    .set("DeviceReportByDeviceCode:" + deviceCode, JSON.toJSONString(voList),
                            120 + ThreadLocalRandom.current().nextInt(30), TimeUnit.SECONDS));
            return Result.ok(voList);
        }
    }

    @Override
    public Result<List<UserReportVO>> getDeviceReportByDeviceCode(String deviceCode) {
        //查询 redis
        String s = redisTemplate.opsForValue().get("DeviceReportByDeviceCode:" + deviceCode);
        if (s != null) {
            return Result.ok(JSON.parseArray(s, UserReportVO.class).stream().toList());
        } else {
            LambdaQueryWrapper<DeviceReservation> eq = new LambdaQueryWrapper<DeviceReservation>()
                    .eq(DeviceReservation::getDeviceCode, deviceCode);
            List<UserReportVO> list = this.list(eq).stream().map(this::toVO).toList();
            pool.submit(() -> redisTemplate.opsForValue()
                    .set("DeviceReportByDeviceCode:" + deviceCode, JSON.toJSONString(list),
                            120 + ThreadLocalRandom.current().nextInt(30), TimeUnit.SECONDS));
            return Result.ok(list);
        }
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



