package com.example.market.mapper;

import com.example.market.entity.Coupon;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CouponMapper {
    @Insert("<script>" +
            "INSERT INTO coupon (" +
            "<if test='goodsId != null'>goods_id,</if>" +
            "amount, issuer_id, status, create_time, type, expire_time, min_amount, total_count, claimed_count, per_user_limit" +
            ") VALUES (" +
            "<if test='goodsId != null'>#{goodsId},</if>" +
            "#{amount}, #{issuerId}, 0, NOW(), #{type}, #{expireTime}, #{minAmount}, #{totalCount}, 0, #{perUserLimit}" +
            ")" +
            "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Coupon coupon);

    @Select("SELECT * FROM coupon WHERE goods_id = #{goodsId} AND status = 0 AND expire_time > NOW() AND claimed_count < total_count ORDER BY create_time DESC")
    List<Coupon> findAvailableListByGoodsId(Long goodsId);

    @Select("SELECT * FROM coupon WHERE goods_id = #{goodsId} AND status = 0 AND expire_time > NOW() AND claimed_count < total_count ORDER BY create_time DESC LIMIT 1")
    Coupon findAvailableByGoodsId(Long goodsId);

    @Select("SELECT * FROM coupon WHERE goods_id = #{goodsId} AND type = 1 AND issuer_id = #{sellerId} AND status = 0 AND expire_time > NOW() LIMIT 1")
    Coupon findAvailableByGoodsIdAndSeller(Long goodsId, Long sellerId);

    // 只增加claimedCount，不改变status，因为优惠券可以被多人领取
    @Update("UPDATE coupon SET claimed_count = claimed_count + 1 WHERE id = #{id} AND expire_time > NOW() AND claimed_count < total_count")
    int claimCoupon(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT * FROM coupon WHERE user_id = #{userId} AND status = 1 ORDER BY create_time DESC")
    List<Coupon> findUserCoupons(Long userId);

    @Select("SELECT * FROM coupon WHERE id = #{id}")
    Coupon findById(Long id);

    @Update("UPDATE coupon SET status = 2, use_time = NOW() WHERE id = #{id} AND status = 1")
    int useCoupon(Long id);

    @Select("SELECT * FROM coupon WHERE issuer_id = #{issuerId} ORDER BY create_time DESC")
    List<Coupon> findByIssuerId(Long issuerId);

    @Update("UPDATE coupon SET status = 2 WHERE id = #{id} AND status = 0")
    int cancelCoupon(Long id);

    @Select("SELECT * FROM coupon WHERE type = 0 AND issuer_id = #{issuerId} AND status = 0 AND expire_time > NOW() AND claimed_count < total_count")
    List<Coupon> findAvailableUniversalByIssuer(Long issuerId);

    // 此方法已废弃，使用UserCouponMapper.countUserClaimedCoupon代替
    @Select("SELECT COUNT(*) FROM coupon WHERE id = #{couponId} AND user_id = #{userId} AND status = 1")
    int countUserClaimedCoupon(@Param("couponId") Long couponId, @Param("userId") Long userId);

    @Select("SELECT * FROM coupon WHERE status = 0 AND expire_time > NOW() ORDER BY create_time DESC")
    List<Coupon> findAllAvailableCoupons();
}