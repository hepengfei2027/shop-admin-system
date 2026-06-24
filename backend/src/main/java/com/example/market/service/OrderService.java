package com.example.market.service;

import com.example.market.dto.OrderDetailDTO;
import com.example.market.entity.Order;

import java.util.List;

public interface OrderService {

    Order createOrder(java.lang.Long goodsId, java.lang.Long buyerId, java.lang.Long addressId, Integer quantity, java.lang.Long couponId,
                      Integer promotionType, java.lang.Long promotionId, java.math.BigDecimal promotionDiscount);

    List<Order> listByBuyer(java.lang.Long buyerId);

    List<Order> listBySeller(java.lang.Long sellerId);

    List<OrderDetailDTO> listByBuyerWithDetails(java.lang.Long buyerId);

    List<OrderDetailDTO> listBySellerWithDetails(java.lang.Long sellerId);

    List<OrderDetailDTO> listAll();

    void updateStatus(java.lang.Long id, Integer status);

    void updateGroupStatus(java.lang.Long id, Integer groupStatus);

    void payOrder(java.lang.Long orderId, java.lang.Long buyerId);

    void payOrderV2(java.lang.Long orderId, java.lang.Long buyerId, String paymentMethod);

    void cancelOrder(java.lang.Long orderId, java.lang.Long userId);

    void shipOrder(java.lang.Long orderId, java.lang.Long sellerId);

    void confirmReceive(java.lang.Long orderId, java.lang.Long buyerId);

    Order getById(java.lang.Long id);

    void applyRefund(java.lang.Long orderId, java.lang.Long buyerId, String remark);

    void approveRefund(java.lang.Long orderId, java.lang.Long sellerId);

    void rejectRefund(java.lang.Long orderId, java.lang.Long sellerId, String remark);

    void confirmShipRefund(java.lang.Long orderId, java.lang.Long buyerId);

    void confirmReceiveRefund(java.lang.Long orderId, java.lang.Long sellerId);

    void cancelRefund(java.lang.Long orderId, java.lang.Long buyerId);
}

