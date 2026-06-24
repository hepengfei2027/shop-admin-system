package com.example.market.mapper;

import com.example.market.entity.Order;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {

    @Select("SELECT * FROM orders WHERE id = #{id}")
    Order findById(java.lang.Long id);

    @Select("SELECT * FROM orders WHERE buyer_id = #{buyerId} ORDER BY create_time DESC")
    List<Order> listByBuyer(java.lang.Long buyerId);

    @Select("SELECT o.*, g.title as goods_title FROM orders o LEFT JOIN goods g ON o.goods_id = g.id WHERE o.seller_id = #{sellerId} ORDER BY o.create_time DESC")
    List<Order> listBySeller(java.lang.Long sellerId);
    
    @Select("SELECT * FROM orders ORDER BY create_time DESC")
    List<Order> listAll();
    
    @Select("SELECT * FROM orders")
    List<Order> findAll();

    @Insert("INSERT INTO orders(goods_id, buyer_id, seller_id, amount, original_amount, discount, coupon_id, coupon_amount, status, address_id, quantity, create_time, promotion_type, promotion_id, promotion_discount, group_status, goods_price, freight, payment_method)"+
            "VALUES(#{goodsId}, #{buyerId}, #{sellerId}, #{amount}, #{originalAmount}, #{discount}, #{couponId}, #{couponAmount}, #{status}, #{addressId}, #{quantity}, NOW(), #{promotionType}, #{promotionId}, #{promotionDiscount}, #{groupStatus}, #{goodsPrice}, #{freight}, #{paymentMethod})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Update("UPDATE orders SET coupon_id = #{couponId}, coupon_amount = #{couponAmount} WHERE id = #{id}")
    int updateCouponInfo(@Param("id") java.lang.Long id, @Param("couponId") java.lang.Long couponId, @Param("couponAmount") java.math.BigDecimal couponAmount);

    @Update("UPDATE orders SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") java.lang.Long id, @Param("status") Integer status);

    @Update("UPDATE orders SET group_status = #{groupStatus} WHERE id = #{id}")
    int updateGroupStatus(@Param("id") java.lang.Long id, @Param("groupStatus") Integer groupStatus);

    @Update("UPDATE orders SET after_sale_status = #{afterSaleStatus} WHERE id = #{id}")
    int updateAfterSaleStatus(@Param("id") java.lang.Long id, @Param("afterSaleStatus") Integer afterSaleStatus);

    @Update("UPDATE orders SET after_sale_status = #{afterSaleStatus}, after_sale_remark = #{afterSaleRemark} WHERE id = #{id}")
    int updateAfterSale(@Param("id") java.lang.Long id, @Param("afterSaleStatus") Integer afterSaleStatus, @Param("afterSaleRemark") String afterSaleRemark);

    @Update("UPDATE orders SET payment_method = #{paymentMethod} WHERE id = #{id}")
    int updatePaymentMethod(@Param("id") java.lang.Long id, @Param("paymentMethod") String paymentMethod);
}

