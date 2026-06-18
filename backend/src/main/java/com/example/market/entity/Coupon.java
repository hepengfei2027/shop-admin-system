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
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime claimTime;
    private LocalDateTime useTime;
    private Integer type;
    private LocalDateTime expireTime;
    private Double minAmount;
    private Integer totalCount;
    private Integer claimedCount;
    private Integer perUserLimit;
}