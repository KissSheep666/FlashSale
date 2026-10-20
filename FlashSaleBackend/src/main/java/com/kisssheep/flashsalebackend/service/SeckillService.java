package com.kisssheep.flashsalebackend.service;

import com.kisssheep.flashsalebackend.dto.SeckillResultDTO;

public interface SeckillService {
    //初始化 将数据库中的库存加载到Redis中
    void initStock();

    //执行具体的秒杀业务 货物扣减 数据库操作等
    String seckill(Long userId, Long goodsId);


    //前端定时轮询请求 以获取真正的数据
    //前端发送userid 以及 goodsId 后端判断是否已经处理完成
    SeckillResultDTO queryResult(Long userId, Long goodsId);
}
