package com.example.market.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Dispute {

    private Long id;
    private Long orderId;
    private Long buyerId;
    private Long sellerId;
    /**
     * 纠纷状态: 0: 无纠纷, 1: 买家申请平台介入, 2: 管理员处理中, 3: 判买家退货, 4: 判卖家退款
     */
    private Integer status;
    private String buyerContent;
    private String buyerImages;
    private String sellerReply;
    private String sellerImages;
    private String adminDecision;
    private String adminRemark;
    private Long adminId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
