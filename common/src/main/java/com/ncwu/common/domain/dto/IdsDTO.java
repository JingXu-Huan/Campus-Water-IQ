package com.ncwu.common.domain.dto;


import com.ncwu.common.validator.annotations.Bloom;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/1/19
 */
@Data
public class IdsDTO {
    @Valid
    @NotNull(message = "设备ID不能为空")
    @Size(min = 1, message = "设备ID不能为空")
    private List<
            @NotBlank(message = "设备ID不能为空")
            @Pattern(
                    regexp = "^[12][1-3](0[1-9]|[1-9][0-9])(0[1-9]|[1-9][0-9])(00[1-9]|0[1-9][0-9]|[1-9][0-9]{2})$",
                    message = "设备ID格式错误"
            )
                    String
            > ids;
}
