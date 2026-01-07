package com.ncwu.ingestgroup.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncwu.ingestgroup.entity.IotDeviceData;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/7
 */
@Mapper
public interface IotDataMapper extends BaseMapper<IotDeviceData> {
}
