package com.ncwu.iotservice.entity.BO;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/7
 */
@Data
@AllArgsConstructor
public class SchoolUsageBO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    Double usage;
    LocalDateTime expireTime;
}
