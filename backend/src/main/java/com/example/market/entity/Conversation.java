package com.example.market.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Conversation {

    private Long id;
    private LocalDateTime createTime;
}