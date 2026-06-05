package com.example.market.controller;

import com.example.market.dto.Result;
import com.example.market.entity.Coupon;
import com.example.market.entity.Goods;
import com.example.market.mapper.CouponMapper;
import com.example.market.mapper.GoodsMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/coupon")
@CrossOrigin
public class CouponController {

    @Resource
    private CouponMapper couponMapper;

    @Resource
    private GoodsMapper goodsMapper;

    @PostMapping("/issue")
    public Result<Coupon> issueCoupon(@RequestParam Long goodsId, @RequestParam Double amount) {
        Goods goods = goodsMapper.findById(goodsId);
        if (goods == null) {
            return Result.fail("商品不存在");
        }

        // 检查是否已有未领取的优惠券
        Coupon existing = couponMapper.findAvailableByGoodsId(goodsId);
        if (existing != null) {
            return Result.fail("该商品已有未领取的优惠券");
        }

        Coupon coupon = new Coupon();
        coupon.setGoodsId(goodsId);
        coupon.setAmount(amount);
        coupon.setIssuerId(goods.getSellerId());
        couponMapper.insert(coupon);

        return Result.ok(coupon);
    }

    @GetMapping("/available")
    public Result<Coupon> getAvailableCoupon(@RequestParam Long goodsId, @RequestParam Long userId) {
        Coupon coupon = couponMapper.findAvailableByGoodsId(goodsId);
        if (coupon != null) {
            return Result.ok(coupon);
        }
        return Result.ok(null);
    }

    @PostMapping("/claim")
    public Result<Coupon> claimCoupon(@RequestParam Long couponId, @RequestParam Long userId) {
        int updated = couponMapper.claimCoupon(couponId, userId);
        if (updated > 0) {
            Coupon coupon = couponMapper.findById(couponId);
            return Result.ok(coupon);
        }
        return Result.fail("优惠券领取失败");
    }

    @GetMapping("/user")
    public Result<List<Coupon>> getUserCoupons(@RequestParam Long userId) {
        List<Coupon> coupons = couponMapper.findUserCoupons(userId);
        return Result.ok(coupons);
    }

    @PostMapping("/use")
    public Result<Coupon> useCoupon(@RequestParam Long couponId) {
        int updated = couponMapper.useCoupon(couponId);
        if (updated > 0) {
            Coupon coupon = couponMapper.findById(couponId);
            return Result.ok(coupon);
        }
        return Result.fail("优惠券使用失败");
    }
}
