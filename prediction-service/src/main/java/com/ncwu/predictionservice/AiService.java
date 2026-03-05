package com.ncwu.predictionservice;


import com.ncwu.common.domain.vo.Result;
import com.ncwu.predictionservice.domain.vo.UsageVO;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/4
 */
public interface AiService {
    Result<UsageVO> predictTomorrowWaterUsage(List<Double> usage, int campus);
}
