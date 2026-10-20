package com.kisssheep.flashsalebackend.service;

import com.kisssheep.flashsalebackend.dto.LoginDto;

public interface UserService {
    String login(LoginDto loginDto);
}
