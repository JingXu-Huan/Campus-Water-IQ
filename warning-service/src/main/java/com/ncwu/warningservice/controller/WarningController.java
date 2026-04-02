package com.ncwu.warningservice.controller;


import com.ncwu.common.domain.vo.Result;
import com.ncwu.warningservice.wechatservice.WarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/4/2
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/warning")
public class WarningController {
    private final WarningService warningService;

    /**
     * 提醒管理员处理用户报修
     */
    @GetMapping("/reWarningUserReport")
    public Result<Boolean> reWarning(String id) {
        return warningService.reWarning(id);
    }
}
