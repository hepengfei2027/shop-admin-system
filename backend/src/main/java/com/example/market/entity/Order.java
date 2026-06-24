package com.example.market.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Order {

    private Long id;
    private Long goodsId;
    private String goodsTitle;
    private Long buyerId;
    private Long sellerId;
    private BigDecimal amount;
    private BigDecimal originalAmount;
    private BigDecimal discount;
    private Long couponId;
    private BigDecimal couponAmount;

    // 营销活动相关
    private Integer promotionType;      // 1=满减, 2=折扣, 3=团购
    private Long promotionId;            // 活动ID
    private BigDecimal promotionDiscount; // 营销活动优惠金额

    // 团购相关状态
    private Integer groupStatus;        // 团购状态: 0=等待拼团, 1=拼团成功, 2=拼团失败

    private Integer status;
    private LocalDateTime createTime;

    private Integer afterSaleStatus;
    private String afterSaleRemark;

    private Long addressId;
    private Integer quantity;

    private String paymentMethod = "wechat";

    // 商品单价和运费
    private BigDecimal goodsPrice;    // 商品单价（下单时的价格）
    private BigDecimal freight;       // 运费
}

