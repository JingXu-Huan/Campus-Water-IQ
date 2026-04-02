package com.ncwu.warningservice.wechatservice;


import com.ncwu.common.domain.IotDeviceEvent;
import com.ncwu.common.domain.vo.Result;
import com.ncwu.warningservice.mapper.DeviceReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/4/2
 */
@Service
@RequiredArgsConstructor
public class WarningServiceImpl implements WarningService {
    private final WeChatNotifyService weChatNotifyService;
    private final DeviceReservationMapper deviceReservationMapper;

    @Override
    public Result<Boolean> reWarning(String id) {
        IotDeviceEvent iotDeviceEvent = deviceReservationMapper.selectById(id);
        if (iotDeviceEvent == null) {
            return Result.fail(false, "404", "告警事件不存在");
        }
        weChatNotifyService.sendMdText(
                iotDeviceEvent.getDeviceCode(),
                iotDeviceEvent.getEventLevel(),
                iotDeviceEvent.getEventDesc(),
                iotDeviceEvent.getEventTime().toString(),
                "用户提醒，请尽快处理该告警事件。"
        );
        return Result.ok(true);
    }
}
