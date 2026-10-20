package com.kisssheep.flashsalebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kisssheep.flashsalebackend.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {

    /**
     * 扣减库存（DB 层面原子操作）
     * 只有当 stock_count > 0 时才扣减成功
     * @return 影响行数（0 = 扣减失败，库存不足）
     */
    @Update("UPDATE goods SET stock_count = stock_count - 1 WHERE id = #{goodsId} AND stock_count > 0")
    int decrStock(@Param("goodsId") Long goodsId);
}
