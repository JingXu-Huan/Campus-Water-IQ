package com.ncwu.predictionservice.functionCalling;

import com.ncwu.common.apis.iot_service.IotDataService;
import com.ncwu.common.domain.vo.Result;
import dev.langchain4j.agent.tool.Tool;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

/**
 * iotService的工具描述类，此类向LLM提供可用的工具描述。
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/12
 */
@Component
public class WaterQueryTools {
    @DubboReference(version = "1.0.0", interfaceClass = IotDataService.class, timeout = 20000)
    private IotDataService iotDataService;

    @Tool("""
            此工具查询三个校区的用水波动指数。school_1 表示花园校区，school_2 表示龙子湖校区，school_3表示江淮校区。
            """)
    Result<Map<String, Double>> getSwings() {
        return iotDataService.getSwings();
    }

    @Tool("此工具查询水质合格率")
    Result<Double> getWaterQuality() {
        return iotDataService.getQualityRate();
    }

    /**
     * 获得所有设备的离线率
     */
    @Tool("""
            此工具查询设备集群的离线率。
            用户提问类似：系统设备的离线率多少？
            你可以调用此工具。
            """)
    Result<Double> getOfflineRate() {
        return iotDataService.getOfflineRate();
    }

    @Tool("""
            此工具用于查询某校区的某时间段用水量数据。
            - school（校区）：1 = 花园校区，2 = 龙子湖校区，3 = 江淮校区
            - start（开始时间）：格式 yyyy-MM-dd HH:mm:ss
            - end（结束时间）：格式 yyyy-MM-dd HH:mm:ss
            【收集参数的原则】
            - 若用户未提供某参数，就查询从今天零点到现在的用水量。
            - 你可以调用一些时间工具获取现在的时间。
            - 三个参数齐全后，立即调用查询工具，无需再次确认。""")
    Result<Double> getSchoolUsage(int school, LocalDateTime start, LocalDateTime end) {
        return iotDataService.getSchoolUsage(school, start, end);
    }

    /**
     * 得到水质评分
     */
    @Tool("""
            此工具用于查询用水单元的水质数据。入参是设备id。需要向用户请求设备id。
            """)
    Result<Double> getWaterQualityScore(String deviceId) {
        return iotDataService.getWaterQualityScore(deviceId);
    }

    @Tool("""
            此工具可以获得整体集群设备的一个健康度评分。
            用户提问类似：整体、系统设备的健康度如何？
            你可以调用此工具。
            """)
    Result<Double> getHealthyScoreOfDevices() {
        return iotDataService.getHealthyScoreOfDevices();
    }

    @Tool("""
            此工具可以返回某校区的下线设备列表。
            需要一个入参：
            - school（校区）：1 = 花园校区，2 = 龙子湖校区，3 = 江淮校区
            当用户询问类似：某校区有哪些设备离线设备？
            你可以调用此工具。
            """)
    Result<Collection<String>> getOffLineList(int campus) {
        return iotDataService.getOffLineList(campus);
    }


    @Tool("""
            此工具可以下载某台设备的原始上报数据报表。
            - 方法需要用户提供设备编码，请主动要求用户输入。
            当用户询问数据报表时，你可以调用此工具。
            """)
    public ResponseEntity<byte[]> getDeviceDatas(String deviceCode) {
        return iotDataService.getDeviceDatas(deviceCode);
    }


    @Tool("""
            此工具可以获取某校区的夜间异常用水量。
            当用户询问类似：花园校区的夜间异常用水量是多少时，你可以调用此工具来获取。
            - 方法需要传入一个校园参数：1 = 花园校区，2 = 龙子湖校区，3 = 江淮校区。
            请主动向用户请求校区，若用户不提供。
            """)
    Result<Double> getUnNormalUsage(@Min(1) @Max(3) int campus) {
        return iotDataService.getUnNormalUsage(campus);
    }

    @Tool("""
            此工具查询三个校区的用水占比。
            当用户询问三个校区的用水占比是多少，你可以调用此工具。
            """)
    Result<Map<Integer, Double>> getCampusRate() {
        return iotDataService.getCampusRate();
    }
}
