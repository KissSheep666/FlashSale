package com.kisssheep.flashsalebackend.dto;

import com.kisssheep.flashsalebackend.entity.Goods;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表 DTO —— 额外包含前端需要的秒杀状态
 * seckillStatus: 0=未开始, 1=进行中, 2=已结束
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoodsDTO {

    private Long id;
    private String name;
    private String title;
    private String image;
    private BigDecimal price;
    private BigDecimal seckillPrice;
    private Integer stockCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** 0=未开始, 1=进行中, 2=已结束 */
    private Integer seckillStatus;

    /**
     * 从 Goods 实体构造 DTO，自动计算秒杀状态
     */
    public static GoodsDTO from(Goods goods) {
        GoodsDTO dto = new GoodsDTO();
        dto.setId(goods.getId());
        dto.setName(goods.getName());
        dto.setTitle(goods.getTitle());
        dto.setImage(goods.getImage());
        dto.setPrice(goods.getPrice());
        dto.setSeckillPrice(goods.getSeckillPrice());
        dto.setStockCount(goods.getStockCount());
        dto.setStartTime(goods.getStartTime());
        dto.setEndTime(goods.getEndTime());

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(goods.getStartTime())) {
            dto.setSeckillStatus(0); // 未开始
        } else if (now.isAfter(goods.getEndTime())) {
            dto.setSeckillStatus(2); // 已结束
        } else {
            dto.setSeckillStatus(1); // 进行中
        }
        return dto;
    }
}
