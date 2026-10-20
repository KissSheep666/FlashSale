package com.kisssheep.flashsalebackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("goods")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Goods {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String title;
    private String image;

    /** 原价 */
    private BigDecimal price;

    /** 秒杀价 */
    private BigDecimal seckillPrice;

    /** 秒杀库存 */
    private Integer stockCount;

    /** 秒杀开始时间 */
    private LocalDateTime startTime;

    /** 秒杀结束时间 */
    private LocalDateTime endTime;

    /** 乐观锁版本号 */
    private Integer version;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
