package com.ncwu.repairservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 设备注册表查询。Redis 是加速层，不能作为设备存在性的唯一依据。
 */
@Mapper
public interface DeviceRegistryMapper {

    @Select("SELECT EXISTS (SELECT 1 FROM virtual_device WHERE device_code = #{deviceCode} " +
            "UNION ALL SELECT 1 FROM iot_device_data WHERE device_code = #{deviceCode})")
    boolean existsByDeviceCode(@Param("deviceCode") String deviceCode);
}
