package com.example.market.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentMedia {

    private Long id;
    private Long commentId;
    private String mediaUrl;
    private String mediaType;
    private LocalDateTime createTime;
}
