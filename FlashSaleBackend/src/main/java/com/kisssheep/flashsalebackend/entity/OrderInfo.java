package com.kisssheep.flashsalebackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("order_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long goodsId;
    private String goodsName;
    private Integer goodsCount;
    private BigDecimal goodsPrice;

    /** 0=新建, 1=已支付, 2=已发货, 3=已完成 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime payTime;
}
