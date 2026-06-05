package com.example.market.mapper;

import com.example.market.entity.ProductComment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductCommentMapper {

    @Select("SELECT * FROM product_comment WHERE id = #{id}")
    ProductComment findById(java.lang.Long id);

    @Select("SELECT * FROM product_comment WHERE goods_id = #{goodsId} AND status = 1 ORDER BY create_time DESC")
    List<ProductComment> listByGoodsId(java.lang.Long goodsId);

    @Select("SELECT * FROM product_comment WHERE order_id = #{orderId}")
    ProductComment findByOrderId(java.lang.Long orderId);

    @Insert("INSERT INTO product_comment(order_id, goods_id, user_id, seller_id, content, rating, status, is_anonymous, create_time) VALUES(#{orderId}, #{goodsId}, #{userId}, #{sellerId}, #{content}, #{rating}, #{status}, #{isAnonymous}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProductComment comment);

    @Update("UPDATE product_comment SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") java.lang.Long id, @Param("status") Integer status);

    @Delete("DELETE FROM product_comment WHERE id = #{id}")
    int delete(java.lang.Long id);
}
