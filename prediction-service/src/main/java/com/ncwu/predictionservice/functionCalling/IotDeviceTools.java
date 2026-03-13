package com.ncwu.predictionservice.functionCalling;


import com.ncwu.common.apis.iot_device.IotDeviceApi;
import com.ncwu.common.domain.vo.Result;
import dev.langchain4j.agent.tool.Tool;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * iotDevice的工具描述类，此类向LLM提供可用的工具描述。
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/12
 */
@Component
public class IotDeviceTools {
    @DubboReference(version = "1.0.0", interfaceClass = IotDeviceApi.class, timeout = 20000)
    private IotDeviceApi iotDeviceApi;

    @Tool("""
            此工具用于检查设备状态，输入设备ID列表，返回设备状态。"
            "会返回一个Map，其中key为设备ID，value为设备状态。"
            "设备的状态是这样的：online,true。" +
            "第一个字段表示是否在线，第二个字段表示是否运行。
            设备的编码要满足：
            ## 设备编码规则（9位）
            
            ```
            deviceType(1位) + campus(1位) + building(2位) + floor(2位) + unit(3位) = 9位
            ```
            
            | 位置 | 含义 | 范围 |
            |------|------|------|
            | 第1位 | 设备类型 | 1=水表, 2=水质传感器 |
            | 第2位 | 校区 | 1=花园校区, 2=龙子湖校区, 3=江淮校区 |
            | 第3-4位 | 楼栋号 | 01-99 |
            | 第5-6位 | 楼层号 | 01-99 |
            | 第7-9位 | 单元号 | 001-999 |
            
            ## 示例
            
            - `10201001001` = 1(类型) + 02(校区) + 01(楼) + 01(层) + 001(单元)
              - 水表 / 龙子湖校区 / 1号楼 / 1层 / 1单元
            
            - `20101001001` = 2(类型) + 01(校区) + 01(楼) + 01(层) + 001(单元)
              - 水质传感器 / 花园校区 / 1号楼 / 1层 / 001
            """)
    public Result<Map<String, String>> checkDeviceStatus(List<String> ids) {
        return iotDeviceApi.checkDeviceStatus(ids);
    }
}
