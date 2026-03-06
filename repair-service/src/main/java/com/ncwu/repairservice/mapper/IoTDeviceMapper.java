package com.ncwu.repairservice.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncwu.repairservice.entity.domain.IotDeviceEvent;
import com.ncwu.repairservice.entity.vo.IotDeviceEventVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/1
 */
@Mapper
public interface IoTDeviceMapper extends BaseMapper<IotDeviceEvent> {
    @Select("select id,device_code,event_desc ,event_level,device_type,event_time from iot_device_event where substring(device_code,2,1)=#{campus} limit 2")
    List<IotDeviceEventVo> getCampusWarnings(Integer campus);
}
