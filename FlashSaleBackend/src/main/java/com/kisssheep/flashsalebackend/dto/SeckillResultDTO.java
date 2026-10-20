package com.kisssheep.flashsalebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 秒杀结果相应
 *
 * -1 未完成
 * > 0 已完成 返回Order_info的主键id
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeckillResultDTO {
    private Long orderId;
}
