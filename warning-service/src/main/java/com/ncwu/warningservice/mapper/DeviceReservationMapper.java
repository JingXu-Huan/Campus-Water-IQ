package com.ncwu.warningservice.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncwu.common.domain.IotDeviceEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/4/2
 */
@Mapper
public interface DeviceReservationMapper extends BaseMapper<IotDeviceEvent> {
}
