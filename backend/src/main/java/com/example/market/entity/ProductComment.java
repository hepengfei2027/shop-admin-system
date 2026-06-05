package com.example.market.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductComment {

    private Long id;
    private Long orderId;
    private Long goodsId;
    private Long userId;
    private Long sellerId;
    private String content;
    private Integer rating;
    private Integer status;
    private Integer isAnonymous;
    private LocalDateTime createTime;
    private List<CommentMedia> media;
}
