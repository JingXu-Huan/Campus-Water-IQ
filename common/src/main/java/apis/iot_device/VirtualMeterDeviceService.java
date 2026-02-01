package apis.iot_device;


import com.ncwu.common.vo.Result;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/1
 */
public interface VirtualMeterDeviceService {

    Result<Integer> getDeviceNums();
}
