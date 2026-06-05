package com.example.market.mapper;

import com.example.market.entity.Coupon;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CouponMapper {
    @Insert("INSERT INTO coupon (goods_id, amount, issuer_id, status, create_time) VALUES (#{goodsId}, #{amount}, #{issuerId}, 0, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Coupon coupon);

    @Select("SELECT * FROM coupon WHERE goods_id = #{goodsId} AND status = 0 LIMIT 1")
    Coupon findAvailableByGoodsId(Long goodsId);

    @Select("SELECT * FROM coupon WHERE goods_id = #{goodsId} AND user_id = #{userId} AND status = 0 LIMIT 1")
    Coupon findAvailableByGoodsAndUser(Long goodsId, Long userId);

    @Update("UPDATE coupon SET user_id = #{userId}, status = 1, claim_time = NOW() WHERE id = #{id} AND status = 0")
    int claimCoupon(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT * FROM coupon WHERE user_id = #{userId} AND status = 1 ORDER BY create_time DESC")
    List<Coupon> findUserCoupons(Long userId);

    @Select("SELECT * FROM coupon WHERE id = #{id}")
    Coupon findById(Long id);

    @Update("UPDATE coupon SET status = 2, use_time = NOW() WHERE id = #{id} AND status = 1")
    int useCoupon(Long id);
}
