package com.kisssheep.flashsalebackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("seckill_order")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long goodsId;
    private Long orderId;

    private LocalDateTime createTime;
}
