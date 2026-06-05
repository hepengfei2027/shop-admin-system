package com.example.market.mapper;

import com.example.market.entity.Dispute;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DisputeMapper {

    @Insert("INSERT INTO dispute(order_id, buyer_id, seller_id, status, buyer_content, buyer_images, create_time) " +
            "VALUES(#{orderId}, #{buyerId}, #{sellerId}, #{status}, #{buyerContent}, #{buyerImages}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Dispute dispute);

    @Select("SELECT * FROM dispute WHERE id = #{id}")
    Dispute findById(Long id);

    @Select("SELECT * FROM dispute WHERE order_id = #{orderId}")
    Dispute findByOrderId(Long orderId);

    @Select("SELECT * FROM dispute ORDER BY create_time DESC")
    List<Dispute> findAll();

    @Update("UPDATE dispute SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE dispute SET seller_reply = #{sellerReply}, seller_images = #{sellerImages}, update_time = NOW() WHERE id = #{id}")
    int updateSellerReply(@Param("id") Long id, @Param("sellerReply") String sellerReply, @Param("sellerImages") String sellerImages);

    @Update("UPDATE dispute SET admin_decision = #{adminDecision}, admin_remark = #{adminRemark}, admin_id = #{adminId}, status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateAdminDecision(@Param("id") Long id, @Param("adminDecision") String adminDecision, @Param("adminRemark") String adminRemark, @Param("adminId") Long adminId, @Param("status") Integer status);
}
