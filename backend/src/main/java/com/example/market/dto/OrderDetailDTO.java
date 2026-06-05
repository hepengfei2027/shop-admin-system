package com.example.market.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDetailDTO {

    private Long id;
    private Long goodsId;
    private Long buyerId;
    private Long sellerId;
    private BigDecimal amount;
    private Integer status;
    private String statusText;
    private Integer afterSaleStatus;
    private String afterSaleStatusText;
    private LocalDateTime createTime;
    
    // 商品信息
    private String goodsName;
    private String goodsDescription;
    private String goodsImage;
    private BigDecimal goodsPrice;
    
    // 买家信息
    private String buyerUsername;
    private String buyerNickname;
    private String buyerAvatar;
    
    // 卖家信息
    private String sellerUsername;
    private String sellerNickname;
    private String sellerAvatar;
}
