package com.ncwu.common.apis.iot_device;


import com.ncwu.common.domain.dto.IdsDTO;
import com.ncwu.common.domain.vo.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/1
 */
public interface VirtualMeterDeviceService {

    Result<Integer> getDeviceNums();

}
