package com.kisssheep.flashsalebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.kisssheep.flashsalebackend.mapper")
public class FlashSaleBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashSaleBackendApplication.class, args);
    }
}
