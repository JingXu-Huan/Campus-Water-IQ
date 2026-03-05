package com.ncwu.predictionservice.domain;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/5
 */
@Data
@AllArgsConstructor
public class UsageBO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    Double usage;
    LocalDateTime expireTime;
}
