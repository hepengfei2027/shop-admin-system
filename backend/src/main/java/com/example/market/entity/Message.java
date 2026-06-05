package com.example.market.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Message {

    private Long id;
    private Long senderId;
    private Long receiverId;
    private Long conversationId;
    private String content;
    private Integer status = 0; // 0: 未读, 1: 已读
    private LocalDateTime createTime;
    private LocalDateTime readTime;
}