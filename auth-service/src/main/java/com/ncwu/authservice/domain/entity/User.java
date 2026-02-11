package com.ncwu.authservice.domain.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;
import java.util.Date;

/**
* 
* @TableName user
*/
@Data
public class User implements Serializable {

    @NotNull(message="[]不能为空")
    private Long id;
    /**
    * 唯一用户ID
    */
    @NotBlank(message="[唯一用户ID]不能为空")
    @Size(max= 100,message="编码长度不能超过100")
    @Length(max= 100,message="编码长度不能超过100")
    private String uid;
    /**
    * 昵称
    */
    @NotBlank(message="[昵称]不能为空")
    @Size(max= 20,message="编码长度不能超过20")
    @Length(max= 20,message="编码长度不能超过20")
    private String nickName;
    /**
    * 邮箱
    */
    @NotBlank(message="[邮箱]不能为空")
    @Size(max= 50,message="编码长度不能超过50")
    @Length(max= 50,message="编码长度不能超过50")
    private String email;
    /**
    * 手机号
    */
    @Size(max= 20,message="编码长度不能超过20")
    @Length(max= 20,message="编码长度不能超过20")
    private String phoneNum;
    /**
    * 密码
    */
    @NotBlank(message="[密码]不能为空")
    @Size(max= 255,message="编码长度不能超过255")
    @Length(max= 255,message="编码长度不能超过255")
    private String password;
    /**
    * 上一次登陆时间
    */
    private Date lastLoginTime;
    /**
    * GitHub用户ID，用于OAuth登录绑定
    */
    private String githubId;
    /**
    * 创建时间
    */
    private Date createDate;
    /**
    * 更新时间
    */
    private Date updateTime;
    /**
    * 1普通用户 2运维 3管理员
    */
    @NotNull(message="[1普通用户 2运维 3管理员]不能为空")
    private Integer userType;
    /**
    * 绑定设备数量
    */
    @NotNull(message="[绑定设备数量]不能为空")
    private Integer bindDeviceCount;
    /**
    * 逻辑删除
    */
    @TableLogic
    @NotNull(message="[逻辑删除]不能为空")
    private Integer deleted;
    /**
    * 1正常 0禁用 2冻结
    */
    @NotNull(message="[1正常 0禁用 2冻结]不能为空")
    private Integer status;
}
