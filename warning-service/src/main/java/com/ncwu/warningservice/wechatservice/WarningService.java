package com.ncwu.warningservice.wechatservice;


import com.ncwu.common.domain.vo.Result;
import org.springframework.stereotype.Service;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/4/2
 */

public interface WarningService {
    Result<Boolean> reWarning(String id);
}
