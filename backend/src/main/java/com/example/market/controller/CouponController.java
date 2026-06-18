package com.example.market.controller;

import com.example.market.dto.Result;
import com.example.market.dto.UserCouponVO;
import com.example.market.entity.Coupon;
import com.example.market.entity.Goods;
import com.example.market.entity.UserCoupon;
import com.example.market.mapper.CouponMapper;
import com.example.market.mapper.GoodsMapper;
import com.example.market.mapper.UserCouponMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/coupon")
@CrossOrigin
public class CouponController {

    @Resource
    private CouponMapper couponMapper;

    @Resource
    private GoodsMapper goodsMapper;

    @Resource
    private UserCouponMapper userCouponMapper;

    @PostMapping("/issue")
    public Result<Coupon> issueCoupon(@RequestParam Long goodsId, @RequestParam Double amount) {
        Goods goods = goodsMapper.findById(goodsId);
        if (goods == null) {
            return Result.fail("商品不存在");
        }

        Coupon coupon = new Coupon();
        coupon.setGoodsId(goodsId);
        coupon.setAmount(amount);
        coupon.setIssuerId(goods.getSellerId());
        coupon.setType(1);
        coupon.setExpireTime(LocalDateTime.now().plusDays(7));
        coupon.setMinAmount(0.0);
        coupon.setTotalCount(100);
        coupon.setClaimedCount(0);
        coupon.setPerUserLimit(1);
        coupon.setStatus(0);
        couponMapper.insert(coupon);

        return Result.ok(coupon);
    }

    @PostMapping("/create")
    public Result<Coupon> createCoupon(@RequestBody Coupon coupon) {

        if (coupon.getType() == 1 && coupon.getGoodsId() == null) {
            return Result.fail("商品优惠券必须指定商品");
        }

        if (coupon.getType() == 1) {
            Goods goods = goodsMapper.findById(coupon.getGoodsId());
            if (goods == null) {
                return Result.fail("商品不存在");
            }
            if (!goods.getSellerId().equals(coupon.getIssuerId())) {
                return Result.fail("只能为自己的商品发放优惠券");
            }
        }

        LocalDateTime expireDateTime;
        try {
            expireDateTime = parseExpireTime(coupon.getExpireTime());
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
        if (expireDateTime.isBefore(LocalDateTime.now())) {
            return Result.fail("到期时间不能早于当前时间");
        }

        Coupon newCoupon = new Coupon();
        newCoupon.setGoodsId(coupon.getType() == 0 ? null : coupon.getGoodsId());
        newCoupon.setAmount(coupon.getAmount());
        newCoupon.setIssuerId(coupon.getIssuerId());
        newCoupon.setType(coupon.getType());
        newCoupon.setExpireTime(expireDateTime);
        newCoupon.setMinAmount(coupon.getMinAmount() != null ? coupon.getMinAmount() : 0.0);
        newCoupon.setTotalCount(coupon.getTotalCount() != null ? coupon.getTotalCount() : 100);
        newCoupon.setClaimedCount(0);
        newCoupon.setPerUserLimit(coupon.getPerUserLimit() != null ? coupon.getPerUserLimit() : 1);
        newCoupon.setStatus(0);
        couponMapper.insert(newCoupon);

        return Result.ok(newCoupon);
    }

    @GetMapping("/available")
    public Result<Coupon> getAvailableCoupon(@RequestParam Long goodsId, @RequestParam Long userId) {
        Coupon coupon = couponMapper.findAvailableByGoodsId(goodsId);
        if (coupon != null) {
            return Result.ok(coupon);
        }
        return Result.ok(null);
    }

    @GetMapping("/available-universal")
    public Result<List<Coupon>> getAvailableUniversalCoupons(@RequestParam Long sellerId, @RequestParam Long userId) {
        List<Coupon> coupons = couponMapper.findAvailableUniversalByIssuer(sellerId);
        return Result.ok(coupons);
    }

    @PostMapping("/claim")
    public Result<Coupon> claimCoupon(@RequestParam Long couponId, @RequestParam Long userId) {
        Coupon coupon = couponMapper.findById(couponId);
        if (coupon == null) {
            return Result.fail("优惠券不存在");
        }

        // 检查优惠券是否已过期
        if (coupon.getExpireTime() != null && coupon.getExpireTime().isBefore(LocalDateTime.now())) {
            return Result.fail("优惠券已过期");
        }

        // 检查优惠券是否已被取消
        if (coupon.getStatus() == 2) {
            return Result.fail("优惠券已被取消");
        }

        // 检查是否已达到总发放数量
        if (coupon.getClaimedCount() >= coupon.getTotalCount()) {
            return Result.fail("优惠券已领完");
        }

        // 检查该用户是否已经领取过（使用user_coupon表）
        int userClaimedCount = userCouponMapper.countUserClaimedCoupon(couponId, userId);
        if (coupon.getPerUserLimit() != null && coupon.getPerUserLimit() > 0) {
            if (userClaimedCount >= coupon.getPerUserLimit()) {
                return Result.fail("已达到领取限制");
            }
        }

        // 在user_coupon表中添加领取记录
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(0);
        int inserted = userCouponMapper.insert(userCoupon);
        if (inserted <= 0) {
            return Result.fail("优惠券领取失败");
        }

        // 更新coupon表的claimedCount
        int updated = couponMapper.claimCoupon(couponId, userId);
        if (updated > 0) {
            Coupon updatedCoupon = couponMapper.findById(couponId);
            return Result.ok(updatedCoupon);
        }
        return Result.fail("优惠券领取失败");
    }

    @GetMapping("/user")
    public Result<List<UserCouponVO>> getUserCoupons(@RequestParam Long userId) {
        List<UserCoupon> userCoupons = userCouponMapper.findUserActiveCoupons(userId);
        List<UserCouponVO> result = new ArrayList<>();
        for (UserCoupon uc : userCoupons) {
            Coupon c = couponMapper.findById(uc.getCouponId());
            if (c != null) {
                result.add(toUserCouponVO(uc, c));
            }
        }
        return Result.ok(result);
    }

    private UserCouponVO toUserCouponVO(UserCoupon uc, Coupon c) {
        UserCouponVO vo = new UserCouponVO();
        vo.setUserCouponId(uc.getId());
        vo.setUserCouponStatus(uc.getStatus());
        vo.setClaimTime(uc.getClaimTime());
        vo.setUseTime(uc.getUseTime());
        vo.setId(c.getId());
        vo.setGoodsId(c.getGoodsId());
        vo.setAmount(c.getAmount());
        vo.setIssuerId(c.getIssuerId());
        vo.setStatus(c.getStatus());
        vo.setCreateTime(c.getCreateTime());
        vo.setType(c.getType());
        vo.setExpireTime(c.getExpireTime());
        vo.setMinAmount(c.getMinAmount());
        vo.setTotalCount(c.getTotalCount());
        vo.setClaimedCount(c.getClaimedCount());
        vo.setPerUserLimit(c.getPerUserLimit());
        return vo;
    }

    private LocalDateTime parseExpireTime(LocalDateTime expireTime) {
        if (expireTime == null) {
            throw new IllegalArgumentException("到期时间不能为空");
        }
        String expireTimeStr = expireTime.toString();
        if (expireTimeStr.endsWith("Z")) {
            return ZonedDateTime.parse(expireTimeStr)
                    .withZoneSameInstant(ZoneId.of("Asia/Shanghai"))
                    .toLocalDateTime();
        }
        return expireTime;
    }

    @GetMapping("/seller")
    public Result<List<Coupon>> getSellerCoupons(@RequestParam Long sellerId) {
        List<Coupon> coupons = couponMapper.findByIssuerId(sellerId);
        return Result.ok(coupons);
    }

    @PostMapping("/use")
    public Result<Coupon> useCoupon(@RequestParam Long couponId, @RequestParam Long userId) {
        // 检查用户是否领取过该优惠券
        UserCoupon userCoupon = userCouponMapper.findByUserAndCoupon(userId, couponId);
        if (userCoupon == null || userCoupon.getStatus() != 0) {
            return Result.fail("用户未领取该优惠券或已使用");
        }

        // 标记user_coupon为已使用
        int updated = userCouponMapper.useCoupon(userCoupon.getId());
        if (updated <= 0) {
            return Result.fail("优惠券使用失败");
        }

        // 更新coupon表（可选，因为已通过user_coupon跟踪）
        Coupon coupon = couponMapper.findById(couponId);
        return Result.ok(coupon);
    }

    @PostMapping("/cancel/{id}")
    public Result<Void> cancelCoupon(@PathVariable Long id) {
        Coupon coupon = couponMapper.findById(id);
        if (coupon == null) {
            return Result.fail("优惠券不存在");
        }

        if (coupon.getStatus() != 0) {
            return Result.fail("只能取消未领取的优惠券");
        }

        int updated = couponMapper.cancelCoupon(id);
        if (updated > 0) {
            return Result.ok(null);
        }
        return Result.fail("取消失败");
    }

    @GetMapping("/all-available")
    public Result<List<Coupon>> getAllAvailableCoupons(
            @RequestParam Long goodsId,
            @RequestParam Long sellerId,
            @RequestParam Long userId) {
        List<Coupon> allCoupons = new ArrayList<>();

        List<Coupon> goodsCoupons = couponMapper.findAvailableListByGoodsId(goodsId);
        if (goodsCoupons != null) {
            allCoupons.addAll(goodsCoupons);
        }

        List<Coupon> universalCoupons = couponMapper.findAvailableUniversalByIssuer(sellerId);
        if (universalCoupons != null) {
            allCoupons.addAll(universalCoupons);
        }

        return Result.ok(allCoupons);
    }
}