package com.ncwu.iotservice.entity.VO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/17
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsageBO {
    private int campus;
    private LocalDateTime time;
    private Double schoolUsage;
}
