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

    @Select("SELECT * FROM orders WHERE seller_id = #{sellerId} ORDER BY create_time DESC")
    List<Order> listBySeller(java.lang.Long sellerId);
    
    @Select("SELECT * FROM orders ORDER BY create_time DESC")
    List<Order> listAll();
    
    @Select("SELECT * FROM orders")
    List<Order> findAll();

    @Insert("INSERT INTO orders(goods_id, buyer_id, seller_id, amount, status, create_time)"+
            "VALUES(#{goodsId}, #{buyerId}, #{sellerId}, #{amount}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Update("UPDATE orders SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") java.lang.Long id, @Param("status") Integer status);

    @Update("UPDATE orders SET after_sale_status = #{afterSaleStatus} WHERE id = #{id}")
    int updateAfterSaleStatus(@Param("id") java.lang.Long id, @Param("afterSaleStatus") Integer afterSaleStatus);

    @Update("UPDATE orders SET after_sale_status = #{afterSaleStatus}, after_sale_remark = #{afterSaleRemark} WHERE id = #{id}")
    int updateAfterSale(@Param("id") java.lang.Long id, @Param("afterSaleStatus") Integer afterSaleStatus, @Param("afterSaleRemark") String afterSaleRemark);
}

