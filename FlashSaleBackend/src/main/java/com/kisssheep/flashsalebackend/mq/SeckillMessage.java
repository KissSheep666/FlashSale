package com.kisssheep.flashsalebackend.mq;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 秒杀信息体 利用MQ传递的信息主体 字段尽可能简单 因为要使用字节流进行传递 所以要进行一个序列化
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeckillMessage implements Serializable {
    // 显式声明版本号 避免编译不一致的情况发生
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long goodsId;
    
}
