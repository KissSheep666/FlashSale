package com.kisssheep.flashsalebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kisssheep.flashsalebackend.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
