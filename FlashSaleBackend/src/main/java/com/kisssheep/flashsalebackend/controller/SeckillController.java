package com.kisssheep.flashsalebackend.controller;

import com.kisssheep.flashsalebackend.common.Result;
import com.kisssheep.flashsalebackend.dto.SeckillResultDTO;
import com.kisssheep.flashsalebackend.interceptor.LoginInterceptor;
import com.kisssheep.flashsalebackend.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    /**
     * 执行秒杀
     * POST /api/seckill/{goodsId}
     */
    @PostMapping("/seckill/{goodsId}")
    public Result<String> seckill(@PathVariable Long goodsId) {
        Long userId = LoginInterceptor.getCurrentUserId();
        String msg = seckillService.seckill(userId, goodsId);
        return Result.success(msg);
    }

    /**
     * 轮询秒杀结果
     * GET /api/order/result/{goodsId}
     * 返回 -1 表示还在排队，其他值表示订单 ID
     */
    @GetMapping("/order/result/{goodsId}")
    public Result<SeckillResultDTO> queryResult(@PathVariable Long goodsId) {
        Long userId = LoginInterceptor.getCurrentUserId();
        SeckillResultDTO result = seckillService.queryResult(userId, goodsId);
        return Result.success(result);
    }
}
