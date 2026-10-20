-- ============================================================
-- FlashSale 压测预备脚本
-- 在压测前执行此脚本，创建 500 个测试用户
-- 密码均为 123456，salt=1a2b3c4d
-- ============================================================

USE flashsale;

-- 清除旧的压测用户（保留初始的 2 个用户 id=1,2）
DELETE FROM seckill_order WHERE user_id > 2;
DELETE FROM order_info WHERE user_id > 2;
DELETE FROM user WHERE id > 2;

-- 重置自增 ID（可选）
ALTER TABLE user AUTO_INCREMENT = 3;

-- 批量插入 500 个压测用户
-- 手机号：13800000001 ~ 13800000500
-- 密码：b7797c6b0187825e79db224b88c12831（123456 + salt 的 MD5）
INSERT INTO user (phone, password, salt, nickname)
SELECT
    CONCAT('138', LPAD(n, 8, '0')),
    'b7797c6b0187825e79db224b88c12831',
    '1a2b3c4d',
    CONCAT('PT', n)
FROM (
    SELECT (@row := @row + 1) AS n
    FROM information_schema.columns a,
         information_schema.columns b,
         (SELECT @row := 2) r
    LIMIT 500
) t;

-- 确认结果
SELECT CONCAT('已插入 ', COUNT(*), ' 个压测用户') AS 结果 FROM user;
