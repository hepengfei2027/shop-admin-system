package com.example.market.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Goods {

    private Long id;
    private String title;
    private String description;
    /**
     * 商品图片地址（URL）
     */
    private String imageUrl;
    private BigDecimal price;
    /**
     * 运费，不填默认包邮（0）
     */
    private BigDecimal freight = BigDecimal.ZERO;
    private Long sellerId;
    /**
     * 0: 审核中, 1: 已上架, 2: 已下架, 3: 已售出
     */
    private Integer status = 0;
    /**
     * 库存数量，默认999
     */
    private Integer stock = 999;
    private LocalDateTime createTime;
    /**
     * 销量
     */
    private Integer sales = 0;
    /**
     * 商品分类
     */
    private String category;
    /**
     * 品牌名称
     */
    private String brandName;
    /**
     * 品牌标签底色
     */
    private String brandColor;
}

