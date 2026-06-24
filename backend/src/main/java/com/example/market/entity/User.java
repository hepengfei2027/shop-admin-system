package com.example.market.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String phone;
    private String avatar;
    private Integer role = 0; // 0: 普通用户, 1: 管理员
    private Integer status = 0; // 0: 正常, 1: 封禁
    private LocalDateTime bannedUntil; // 封禁截止时间
    private LocalDateTime createTime;
    
    private Integer memberLevel = 1; // 会员等级 1-6
    private Integer experience = 0; // 经验值
    private Double discount = 1.0; // 会员折扣 1.0 = 100%
    private java.math.BigDecimal balance = java.math.BigDecimal.ZERO; // 账户余额
}

