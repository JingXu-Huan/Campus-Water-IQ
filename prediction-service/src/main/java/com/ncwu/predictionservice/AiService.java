package com.ncwu.predictionservice;


import com.ncwu.common.domain.vo.Result;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/4
 */
public interface AiService {
    Result<Double> predictTomorrowWaterUsage(@Min(1) @Max(3) int campus, List<Double> usage);
}
