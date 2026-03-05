package com.ncwu.predictionservice.impl;


import com.ncwu.common.domain.vo.Result;
import com.ncwu.predictionservice.AiService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/5
 */
@Service
public class AiServiceImpl implements AiService {
    @Override
    public Result<Double> predictTomorrowWaterUsage(int campus, List<Double> usage) {
        return null;
    }
}
