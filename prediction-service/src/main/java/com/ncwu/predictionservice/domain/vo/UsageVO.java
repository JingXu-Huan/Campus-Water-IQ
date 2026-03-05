package com.ncwu.predictionservice.domain.vo;


import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/5
 */
@Data
@AllArgsConstructor
public class UsageVO {
    int campus;
    double usage;
}
