package com.ncwu.iotdevice.Bo;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/12/21
 */
@Data
@AllArgsConstructor
public class DeviceIdList {
    List<String> meterDeviceIds;
    List<String> waterQualityDeviceIds;
}
