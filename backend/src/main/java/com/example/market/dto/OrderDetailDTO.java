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

    // 收货地址信息
    private String addressName;
    private String addressPhone;
    private String addressProvince;
    private String addressCity;
    private String addressDistrict;
    private String addressDetail;

    // 购买数量
    private Integer quantity;
    private BigDecimal originalAmount;
    private BigDecimal discount;
    private Long couponId;
    private BigDecimal couponAmount;

    private String paymentMethod;

    // 商品单价和运费
    private BigDecimal freight;

    // 营销活动相关
    private Integer promotionType;      // 1=满减, 2=折扣, 3=团购
    private Long promotionId;            // 活动ID
    private BigDecimal promotionDiscount; // 营销活动优惠金额

    // 团购相关状态
    private Integer groupStatus;        // 团购状态: 0=等待拼团, 1=拼团成功, 2=拼团失败
}
