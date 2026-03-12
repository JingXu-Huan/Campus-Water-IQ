package com.ncwu.predictionservice.functionCalling;

import com.ncwu.common.apis.iot_service.IotDataService;
import com.ncwu.common.domain.vo.Result;
import dev.langchain4j.agent.tool.Tool;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/12
 */
@Component
public class WaterQueryTools {
    @DubboReference(version = "1.0.0", interfaceClass = IotDataService.class, timeout = 20000)
    private IotDataService iotDataService;

    @Tool("此工具查询三个校区的用水波动指数。school_1 表示花园校区，school_2 表示龙子湖校区，school_3表示江淮校区。")
    Result<Map<String, Double>> getSwings() {
        return iotDataService.getSwings();
    }

}
