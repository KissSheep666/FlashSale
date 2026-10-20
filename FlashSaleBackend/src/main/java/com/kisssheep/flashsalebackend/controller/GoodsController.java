package com.kisssheep.flashsalebackend.controller;

import com.kisssheep.flashsalebackend.common.Result;
import com.kisssheep.flashsalebackend.dto.GoodsDTO;
import com.kisssheep.flashsalebackend.service.GoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goods")
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService goodsService;

    /**
     * 返回所有商品列表（含秒杀状态）
     */
    @GetMapping
    public Result<List<GoodsDTO>> listGoods() {
        return Result.success(goodsService.listGoods());
    }

    /**
     * 获取单个商品详情
     */
    @GetMapping("/{goodsId}")
    public Result<GoodsDTO> getGoods(@PathVariable Long goodsId) {
        return Result.success(goodsService.getGoodsById(goodsId));
    }
}
