package com.ncwu.iotservice.entity.BO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 此BO用于结构化向AI发起提问
 *
 * @author jingxu
 * @version 1.0.0
 * @since 2026/3/8
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToAIBO {
    //花园校区
    List<Double> HY = new ArrayList<>(7);
    //龙子湖校区
    List<Double> LH = new ArrayList<>(7);
    //江淮校区
    List<Double> JH = new ArrayList<>(7);
}
