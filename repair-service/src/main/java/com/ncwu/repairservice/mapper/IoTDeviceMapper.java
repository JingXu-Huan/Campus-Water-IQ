package com.ncwu.repairservice.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncwu.repairservice.entity.domain.IotDeviceEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/1
 */
@Mapper
public interface IoTDeviceMapper extends BaseMapper<IotDeviceEvent> {
}
