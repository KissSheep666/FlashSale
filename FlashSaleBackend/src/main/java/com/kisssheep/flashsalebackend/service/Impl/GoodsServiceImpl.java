package com.kisssheep.flashsalebackend.service.Impl;

import com.kisssheep.flashsalebackend.common.CodeMsg;
import com.kisssheep.flashsalebackend.common.GlobalException;
import com.kisssheep.flashsalebackend.dto.GoodsDTO;
import com.kisssheep.flashsalebackend.entity.Goods;
import com.kisssheep.flashsalebackend.mapper.GoodsMapper;
import com.kisssheep.flashsalebackend.service.GoodsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsServiceImpl implements GoodsService {

    private final GoodsMapper goodsMapper;

    @Override
    public List<GoodsDTO> listGoods() {
        List<Goods> goodsList = goodsMapper.selectList(null);
        return goodsList.stream()
                .map(GoodsDTO::from)
                .collect(Collectors.toList());
    }

    @Override
    public GoodsDTO getGoodsById(Long goodsId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new GlobalException(CodeMsg.PARAM_ERROR);
        }
        return GoodsDTO.from(goods);
    }
}
