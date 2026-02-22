package com.ncwu.iotservice.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncwu.iotservice.entity.IotDeviceData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/20
 */
@Mapper
public interface IoTDeviceDataMapper extends BaseMapper<IotDeviceData> {

    @Select("select sum(status='offline')*1.0/count(*) from virtual_device where status!='scrap'")
    double getOffLineRate();

    @Select("select count(*) from iot_device_event")
    int countAll();

    @Select("select count(*) from iot_device_event where handled_flag = 0")
    int countUnhandledAbnormalEvents();
}
