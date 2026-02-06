package com.ncwu.repairservice.entity.domain;

import javax.validation.constraints.Size;
import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
* 设备和用户对应关系表
* @TableName device_user
*/
@Data
public class DeviceUser implements Serializable {
    @Serial
    private final static long serialVersionUID = 1L;

    private Integer id;
    /**
    * 设备ID
    */
    @Size(max= 10,message="编码长度不能超过10")
    @Length(max= 10,message="编码长度不能超过10")
    private String deviceCode;
    /**
    * 用户ID
    */
    @NotBlank(message="[uid]不能为空")
    @Size(max= 40,message="编码长度不能超过40")
    @Length(max= 40,message="编码长度不能超过40")
    private String uid;
}
