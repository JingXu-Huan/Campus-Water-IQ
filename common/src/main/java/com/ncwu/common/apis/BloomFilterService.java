package com.ncwu.common.apis;


import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/18
 */

public interface BloomFilterService {

    boolean mightContains(List<String> ids);

    boolean add(List<String> ids);
}
