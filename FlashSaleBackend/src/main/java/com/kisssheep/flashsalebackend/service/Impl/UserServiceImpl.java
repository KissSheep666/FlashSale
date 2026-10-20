package com.kisssheep.flashsalebackend.service.Impl;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kisssheep.flashsalebackend.common.CodeMsg;
import com.kisssheep.flashsalebackend.common.GlobalException;
import com.kisssheep.flashsalebackend.dto.LoginDto;
import com.kisssheep.flashsalebackend.entity.User;
import com.kisssheep.flashsalebackend.mapper.UserMapper;
import com.kisssheep.flashsalebackend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;

    @Override
    public String login(LoginDto loginDto) {
        //按照手机号进行用户的查询
        //利用构造器进行实现 字段名,变量名 进行绑定
        LambdaQueryWrapper<User> eq = new LambdaQueryWrapper<User>().eq(User::getPhone, loginDto.getPhone());
        User user = userMapper.selectOne(eq);
        if (user == null){
            throw new GlobalException(CodeMsg.USER_NOT_FOUND);
        }

        //如果用户存在 进行下一步校验,进行具体密码的比对
        //将原始的用户密码加盐 加密 之后与数据库里的加密密码进行比对
        String s = user.getSalt() + loginDto.getPassword();
        String inputHash = DigestUtils.md5DigestAsHex(s.getBytes(StandardCharsets.UTF_8));
        if (!inputHash.equals(user.getPassword())){
            throw new GlobalException(CodeMsg.PASSWORD_ERROR);
        }
        //获取登录成功后的token
        //String token = UUID.randomUUID().toString().replace("-", "");

        //使后面修改使用用Sa-Token生成token
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();
        //将token 以及userID 进行映射处理存进Redis
        log.info("用户登录成功,id:{},phone:{}", user.getId(), user.getPhone());
        return token;
    }
}
