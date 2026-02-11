package com.ncwu.authservice.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ncwu.authservice.domain.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2026/2/7
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
