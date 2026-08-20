package com.ncwu.repairservice.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncwu.common.domain.IotDeviceEvent;
import com.ncwu.repairservice.entity.vo.IotDeviceEventVo;
import org.apache.ibatis.annotations.Delete;
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
    @Select("select id,device_code,event_desc ,event_level,device_type,event_time from iot_device_event where substring(device_code,2,1)=#{campus} order by event_time desc limit 2")
    List<IotDeviceEventVo> getCampusWarnings(Integer campus);

    @Delete("delete from iot_device_event where substring(device_code,2,1)=#{campus}")
    int deleteByCampus(Integer campus);

    @Select("select count(*) from iot_device_event")
    int selectAllNums();
}
