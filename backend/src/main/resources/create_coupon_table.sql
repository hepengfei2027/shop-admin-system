CREATE TABLE IF NOT EXISTS coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    goods_id BIGINT NOT NULL COMMENT '商品ID',
    amount DOUBLE NOT NULL COMMENT '优惠金额',
    issuer_id BIGINT NOT NULL COMMENT '发放者ID（卖家ID）',
    user_id BIGINT COMMENT '领取者ID',
    status INT DEFAULT 0 COMMENT '状态：0-未领取，1-已领取未使用，2-已使用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    claim_time DATETIME COMMENT '领取时间',
    use_time DATETIME COMMENT '使用时间',
    INDEX idx_goods_id (goods_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';
