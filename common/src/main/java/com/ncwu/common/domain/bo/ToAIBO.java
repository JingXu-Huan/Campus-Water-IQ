package com.ncwu.common.domain.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 接收 IoT-service 返回的用水数据
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/8
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToAIBO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    
    // 花园校区
    List<Double> HY = new ArrayList<>(7);
    // 龙子湖校区
    List<Double> LH = new ArrayList<>(7);
    // 江淮校区
    List<Double> JH = new ArrayList<>(7);
}
