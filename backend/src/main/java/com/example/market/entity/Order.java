package com.example.market.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Order {

    private Long id;
    private Long goodsId;
    private Long buyerId;
    private Long sellerId;
    private BigDecimal amount;
    /**
     * 0: 待付款, 1: 待发货, 2: 待收货, 3: 已完成, 4: 已取消
     */
    private Integer status;
    private LocalDateTime createTime;
    /**
     * 售后状态: 0: 无售后, 1: 待商家确认, 2: 商家同意退货, 3: 买家已发货, 4: 商家已收货/退款成功, 5: 商家拒绝退货
     */
    private Integer afterSaleStatus;
    /**
     * 售后备注
     */
    private String afterSaleRemark;
}

