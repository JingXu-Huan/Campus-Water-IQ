package com.ncwu.iotdevice.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncwu.iotdevice.domain.entity.VirtualDevice;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/21
 */
@Mapper
public interface DeviceMapper extends BaseMapper<VirtualDevice> {
}
