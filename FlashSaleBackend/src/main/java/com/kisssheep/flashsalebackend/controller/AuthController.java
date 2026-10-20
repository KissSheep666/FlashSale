package com.kisssheep.flashsalebackend.controller;

import com.kisssheep.flashsalebackend.common.Result;
import com.kisssheep.flashsalebackend.dto.LoginDto;
import com.kisssheep.flashsalebackend.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/user")
//不需要每次都进行注入
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;


    /**
     * 登录功能的实现 登录成功后返回token
     */
    @PostMapping("/login")
    //@Valid触发类中的数据的校验
    public Result<String> login(@Valid @RequestBody LoginDto loginDto) {
        String token = userService.login(loginDto);
        return Result.success(token);
    }



}
