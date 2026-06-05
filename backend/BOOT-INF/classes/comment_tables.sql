-- 商品评论主表
CREATE TABLE IF NOT EXISTS `product_comment` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `goods_id` BIGINT NOT NULL COMMENT '商品ID',
  `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
  `seller_id` BIGINT NOT NULL COMMENT '商家ID',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `rating` INT NOT NULL DEFAULT 5 COMMENT '评分 1-5',
  `status` INT NOT NULL DEFAULT 1 COMMENT '状态 0-删除 1-正常 2-屏蔽',
  `is_anonymous` INT NOT NULL DEFAULT 0 COMMENT '是否匿名 0-否 1-是',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_goods_id` (`goods_id`),
  INDEX `idx_order_id` (`order_id`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评论表';

-- 评论图片/视频表
CREATE TABLE IF NOT EXISTS `comment_media` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `comment_id` BIGINT NOT NULL COMMENT '评论ID',
  `media_url` VARCHAR(500) NOT NULL COMMENT '媒体URL',
  `media_type` VARCHAR(20) NOT NULL COMMENT '媒体类型 image/video',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_comment_id` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论媒体表';

-- 评论回复表
CREATE TABLE IF NOT EXISTS `comment_reply` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `comment_id` BIGINT NOT NULL COMMENT '评论ID',
  `user_id` BIGINT NOT NULL COMMENT '回复用户ID',
  `content` TEXT NOT NULL COMMENT '回复内容',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_comment_id` (`comment_id`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论回复表';
