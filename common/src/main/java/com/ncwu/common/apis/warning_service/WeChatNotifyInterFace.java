package com.ncwu.common.apis.warning_service;


/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/5
 */
public interface WeChatNotifyInterFace {
    void sendMdText(String deviceCode, String level, String desc, String time, String suggestion);

    void sendText(String content);
}
