package com.example.market.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserConversation {

    private Long id;
    private Long userId;
    private Long conversationId;
    private Integer status = 1; // 1: 正常, 0: 软删除
    private LocalDateTime lastReadTime;
    private LocalDateTime createTime;
}