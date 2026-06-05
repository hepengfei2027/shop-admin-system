package com.example.market.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Coupon {
    private Long id;
    private Long goodsId;
    private Double amount;
    private Long issuerId;
    private Long userId;
    private Integer status; // 0: 未领取, 1: 已领取未使用, 2: 已使用
    private LocalDateTime createTime;
    private LocalDateTime claimTime;
    private LocalDateTime useTime;
}
