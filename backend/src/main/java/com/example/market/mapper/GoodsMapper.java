package com.example.market.mapper;

import com.example.market.entity.Goods;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface GoodsMapper {

    @Select("SELECT * FROM goods WHERE id = #{id}")
    Goods findById(java.lang.Long id);

    @Select("SELECT * FROM goods WHERE status = 1 AND stock > 0 ORDER BY create_time DESC")
    List<Goods> listOnSale();

    @Select("SELECT * FROM goods WHERE status = 1 AND stock > 0 AND title LIKE CONCAT('%', #{keyword}, '%') ORDER BY create_time DESC")
    List<Goods> search(String keyword);

    @Insert("INSERT INTO goods(title, description, image_url, price, freight, seller_id, status, stock, create_time) VALUES(#{title}, #{description}, #{imageUrl}, #{price}, #{freight}, #{sellerId}, #{status}, #{stock}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Goods goods);

    @Update("UPDATE goods SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}")
    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    @Update("UPDATE goods SET stock = stock + #{quantity} WHERE id = #{id}")
    int increaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    @Update("UPDATE goods SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") java.lang.Long id, @Param("status") Integer status);

    @Select("SELECT * FROM goods WHERE seller_id = #{userId} ORDER BY create_time DESC")
    List<Goods> listByUserId(java.lang.Long userId);

    @Select("SELECT * FROM goods WHERE status = 0 ORDER BY create_time DESC")
    List<Goods> listPending();

    @Delete("DELETE FROM goods WHERE id = #{id}")
    int delete(java.lang.Long id);
    
    @Select("SELECT * FROM goods")
    List<Goods> findAll();
    
    @Select("SELECT COUNT(*) FROM goods")
    long count();
}

