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

    @Select("SELECT * FROM goods WHERE status = 1 AND stock > 0 AND (title LIKE CONCAT('%', #{keyword}, '%') OR category LIKE CONCAT('%', #{keyword}, '%')) ORDER BY create_time DESC")
    List<Goods> search(String keyword);

    @Select("SELECT DISTINCT brand_name, brand_color FROM goods WHERE brand_name IS NOT NULL AND brand_name != '' AND status = 1 ORDER BY brand_name")
    List<Goods> listBrands();

    @Select("SELECT * FROM goods WHERE status = 1 AND stock > 0 " +
            "AND (title LIKE CONCAT('%', #{keyword}, '%') OR category LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (brand_name = #{brand} OR #{brand} IS NULL OR #{brand} = '') " +
            "AND (price >= #{minPrice} OR #{minPrice} IS NULL OR #{minPrice} = 0) " +
            "AND (price <= #{maxPrice} OR #{maxPrice} IS NULL OR #{maxPrice} = 0) " +
            "ORDER BY create_time DESC")
    List<Goods> searchWithFilter(@Param("keyword") String keyword, 
                                 @Param("brand") String brand, 
                                 @Param("minPrice") java.math.BigDecimal minPrice, 
                                 @Param("maxPrice") java.math.BigDecimal maxPrice);

    @Insert("INSERT INTO goods(title, description, image_url, price, freight, seller_id, status, stock, create_time, category, brand_name, brand_color) VALUES(#{title}, #{description}, #{imageUrl}, #{price}, #{freight}, #{sellerId}, #{status}, #{stock}, NOW(), #{category}, #{brandName}, #{brandColor})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Goods goods);

    @Update("UPDATE goods SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}")
    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    @Update("UPDATE goods SET stock = stock + #{quantity} WHERE id = #{id}")
    int increaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    @Update("UPDATE goods SET title = #{title}, description = #{description}, image_url = #{imageUrl}, price = #{price}, freight = #{freight}, stock = #{stock}, category = #{category}, brand_name = #{brandName}, brand_color = #{brandColor}, status = #{status} WHERE id = #{id}")
    int update(Goods goods);

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

