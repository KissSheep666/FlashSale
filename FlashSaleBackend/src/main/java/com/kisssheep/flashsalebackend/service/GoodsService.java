package com.kisssheep.flashsalebackend.service;

import com.kisssheep.flashsalebackend.dto.GoodsDTO;

import java.util.List;

public interface GoodsService {

    /** 获取所有商品列表（含秒杀状态） */
    List<GoodsDTO> listGoods();

    /** 获取单个商品详情 */
    GoodsDTO getGoodsById(Long goodsId);
}
