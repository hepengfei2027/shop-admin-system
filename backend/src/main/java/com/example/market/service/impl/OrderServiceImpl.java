package com.example.market.service.impl;

import com.example.market.dto.OrderDetailDTO;
import com.example.market.entity.Address;
import com.example.market.entity.Coupon;
import com.example.market.entity.Goods;
import com.example.market.entity.Order;
import com.example.market.entity.PromotionActivity;
import com.example.market.entity.PromotionDiscount;
import com.example.market.entity.User;
import com.example.market.mapper.AddressMapper;
import com.example.market.mapper.CouponMapper;
import com.example.market.mapper.GoodsMapper;
import com.example.market.mapper.OrderMapper;
import com.example.market.mapper.PromotionActivityMapper;
import com.example.market.mapper.PromotionDiscountMapper;
import com.example.market.mapper.UserCouponMapper;
import com.example.market.mapper.UserMapper;
import com.example.market.service.GoodsService;
import com.example.market.service.OrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private GoodsMapper goodsMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private AddressMapper addressMapper;

    @Resource
    private GoodsService goodsService;

    @Resource
    private CouponMapper couponMapper;

    @Resource
    private UserCouponMapper userCouponMapper;

    @Resource
    private PromotionActivityMapper promotionActivityMapper;

    @Resource
    private PromotionDiscountMapper promotionDiscountMapper;

    @Override
    public Order createOrder(java.lang.Long goodsId, java.lang.Long buyerId, java.lang.Long addressId, Integer quantity, java.lang.Long couponId,
                            Integer promotionType, java.lang.Long promotionId, java.math.BigDecimal promotionDiscount) {
        System.out.println("=== 创建订单 ===");
        System.out.println("goodsId: " + goodsId);
        System.out.println("buyerId: " + buyerId);
        System.out.println("addressId: " + addressId);
        System.out.println("quantity: " + quantity);
        System.out.println("couponId: " + couponId);
        System.out.println("promotionType: " + promotionType);
        System.out.println("promotionId: " + promotionId);
        System.out.println("promotionDiscount: " + promotionDiscount);

        Goods goods = goodsMapper.findById(goodsId);
        if (goods == null || goods.getStatus() != 1) {
            throw new RuntimeException("商品不存在或未上架");
        }
        if (goods.getSellerId().equals(buyerId)) {
            throw new RuntimeException("不能购买自己的商品");
        }

        int buyQuantity = (quantity != null && quantity > 0) ? quantity : 1;
        if (!goodsService.decreaseStock(goodsId, buyQuantity)) {
            throw new RuntimeException("库存不足");
        }
        Order order = new Order();
        order.setGoodsId(goodsId);
        order.setBuyerId(buyerId);
        order.setSellerId(goods.getSellerId());
        order.setAddressId(addressId);
        order.setQuantity(buyQuantity);
        System.out.println("设置addressId: " + addressId);
        System.out.println("设置quantity: " + buyQuantity);

        BigDecimal price = goods.getPrice() == null ? BigDecimal.ZERO : goods.getPrice();
        BigDecimal freight = goods.getFreight() == null ? BigDecimal.ZERO : goods.getFreight();

        // 保存商品单价和运费
        order.setGoodsPrice(price);
        order.setFreight(freight);

        BigDecimal goodsTotal = price.multiply(BigDecimal.valueOf(buyQuantity));
        order.setOriginalAmount(goodsTotal.add(freight));

        User buyer = userMapper.findById(buyerId);
        double discount = buyer != null && buyer.getDiscount() != null ? buyer.getDiscount() : 1.0;

        BigDecimal discountedGoodsTotal;
        
        // 应用营销活动折扣（限时折扣或满减或团购）
        BigDecimal promotionAmount = BigDecimal.ZERO;
        if (promotionType != null && promotionDiscount != null) {
            if (promotionType == 2) {
                // 限时折扣：折扣已经在价格中体现，这里记录优惠金额
                promotionAmount = promotionDiscount;
                discountedGoodsTotal = goodsTotal.multiply(BigDecimal.valueOf(discount));
            } else if (promotionType == 1) {
                // 满减：直接减免，前端已计算好优惠金额
                promotionAmount = promotionDiscount;
                // 满减订单：先应用会员折扣
                discountedGoodsTotal = goodsTotal.multiply(BigDecimal.valueOf(discount));
                // 检查是否需要应用限时折扣（满减可与限时折扣叠加）
                PromotionActivity discountAct = promotionActivityMapper.findActiveByGoodsIdAndType(goodsId, 2);
                if (discountAct != null) {
                    PromotionDiscount discountRule = promotionDiscountMapper.findByActivityId(discountAct.getId());
                    if (discountRule != null && discountRule.getDiscountRate() != null) {
                        discountedGoodsTotal = discountedGoodsTotal.multiply(discountRule.getDiscountRate());
                    }
                }
            } else if (promotionType == 3) {
                // 团购：使用团购价计算商品总额，不应用会员折扣
                // promotionDiscount = 原价总额 - 团购价总额
                promotionAmount = promotionDiscount;
                // 团购订单直接使用团购价，不再应用会员折扣
                discountedGoodsTotal = goodsTotal.subtract(promotionAmount);
            } else {
                discountedGoodsTotal = goodsTotal.multiply(BigDecimal.valueOf(discount));
            }
            order.setPromotionType(promotionType);
            order.setPromotionId(promotionId);
            order.setPromotionDiscount(promotionAmount);
        } else {
            discountedGoodsTotal = goodsTotal.multiply(BigDecimal.valueOf(discount));
        }

        BigDecimal couponAmount = BigDecimal.ZERO;
        if (couponId != null) {
            Coupon coupon = couponMapper.findById(couponId);
            if (coupon == null) {
                throw new RuntimeException("优惠券不存在");
            }
            if (coupon.getStatus() != null && coupon.getStatus() == 2) {
                throw new RuntimeException("优惠券已失效");
            }
            if (coupon.getExpireTime() != null && coupon.getExpireTime().isBefore(java.time.LocalDateTime.now())) {
                throw new RuntimeException("优惠券已过期");
            }
            if (!coupon.getIssuerId().equals(goods.getSellerId())) {
                throw new RuntimeException("优惠券不适用于该商家");
            }
            com.example.market.entity.UserCoupon userCoupon = userCouponMapper.findByUserAndCoupon(buyerId, couponId);
            if (userCoupon == null || userCoupon.getStatus() != 0) {
                throw new RuntimeException("您尚未领取该优惠券或已使用");
            }
            if (coupon.getType() == 1 && (coupon.getGoodsId() == null || !coupon.getGoodsId().equals(goodsId))) {
                throw new RuntimeException("优惠券不适用于该商品");
            }
            if (coupon.getMinAmount() != null && discountedGoodsTotal.compareTo(BigDecimal.valueOf(coupon.getMinAmount())) < 0) {
                throw new RuntimeException("未满足优惠券最低消费条件");
            }
            couponAmount = BigDecimal.valueOf(coupon.getAmount());
            order.setCouponId(couponId);
            order.setCouponAmount(couponAmount);
        }

        // 计算最终金额：折扣价 + 运费 - 满减/折扣 - 优惠券
        BigDecimal finalAmount = discountedGoodsTotal.add(freight).subtract(promotionAmount).subtract(couponAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }
        order.setAmount(finalAmount);
        order.setDiscount(BigDecimal.valueOf(discount));

        order.setStatus(0);
        
        // 如果是团购订单，设置初始状态为"等待拼团"
        if (promotionType != null && promotionType == 3) {
            order.setGroupStatus(0); // 0=等待拼团
        }
        
        orderMapper.insert(order);

        System.out.println("订单创建成功，ID: " + order.getId());
        System.out.println("订单addressId: " + order.getAddressId());
        System.out.println("订单quantity: " + order.getQuantity());
        System.out.println("订单amount: " + order.getAmount());
        System.out.println("订单couponId: " + order.getCouponId());
        System.out.println("订单couponAmount: " + order.getCouponAmount());
        System.out.println("订单promotionType: " + order.getPromotionType());
        System.out.println("订单promotionDiscount: " + order.getPromotionDiscount());
        System.out.println("订单groupStatus: " + order.getGroupStatus());

        return order;
    }

    @Override
    public void payOrder(java.lang.Long orderId, java.lang.Long buyerId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (order.getStatus() != 0) {
            throw new RuntimeException("订单状态不正确，无法付款");
        }
        orderMapper.updateStatus(orderId, 1);

        // 如果是团购订单，设置groupStatus为0（等待拼团）
        if (order.getPromotionType() != null && order.getPromotionType() == 3) {
            orderMapper.updateGroupStatus(orderId, 0);
        }

        if (order.getCouponId() != null) {
            // 使用user_coupon表来标记优惠券已使用
            com.example.market.entity.UserCoupon userCoupon = userCouponMapper.findByUserAndCoupon(order.getBuyerId(), order.getCouponId());
            if (userCoupon != null) {
                userCouponMapper.useCoupon(userCoupon.getId());
            }
        }
    }

    @Override
    public void cancelOrder(java.lang.Long orderId, java.lang.Long userId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (order.getStatus() != 0) {
            throw new RuntimeException("订单状态不正确，无法取消");
        }
        orderMapper.updateStatus(orderId, 4);
        goodsService.increaseStock(order.getGoodsId(), 1);
    }

    @Override
    public void shipOrder(java.lang.Long orderId, java.lang.Long sellerId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getSellerId().equals(sellerId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (order.getStatus() != 1) {
            throw new RuntimeException("订单状态不正确，无法发货");
        }
        orderMapper.updateStatus(orderId, 2);
    }

    @Override
    public void confirmReceive(java.lang.Long orderId, java.lang.Long buyerId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (order.getStatus() != 2) {
            throw new RuntimeException("订单状态不正确，无法确认收货");
        }
        orderMapper.updateStatus(orderId, 3);
        
        updateMemberExperience(buyerId, order.getAmount());
    }
    
    private void updateMemberExperience(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        
        User user = userMapper.findById(userId);
        if (user == null) {
            return;
        }
        
        int currentExp = user.getExperience() != null ? user.getExperience() : 0;
        int addExp = (int) Math.floor(amount.doubleValue());
        int newExp = currentExp + addExp;
        
        MemberConfig.MemberLevel level = MemberConfig.getLevelByExp(newExp);
        
        userMapper.updateMemberInfo(userId, level.getLevel(), newExp, level.getDiscount());
    }

    @Override
    public List<Order> listByBuyer(java.lang.Long buyerId) {
        return orderMapper.listByBuyer(buyerId);
    }

    @Override
    public List<Order> listBySeller(java.lang.Long sellerId) {
        List<Order> orders = orderMapper.listBySeller(sellerId);
        // 过滤掉团购订单中groupStatus=0（等待拼团）的订单
        orders.removeIf(order ->
            order.getPromotionType() != null &&
            order.getPromotionType() == 3 &&
            order.getGroupStatus() != null &&
            order.getGroupStatus() == 0
        );
        return orders;
    }

    @Override
    public List<OrderDetailDTO> listByBuyerWithDetails(java.lang.Long buyerId) {
        List<Order> orders = orderMapper.listByBuyer(buyerId);
        List<OrderDetailDTO> result = new ArrayList<>();
        for (Order order : orders) {
            result.add(buildOrderDetailDTO(order));
        }
        return result;
    }

    @Override
    public List<OrderDetailDTO> listBySellerWithDetails(java.lang.Long sellerId) {
        List<Order> orders = orderMapper.listBySeller(sellerId);
        // 过滤掉团购订单中groupStatus=0（等待拼团）的订单
        orders.removeIf(order ->
            order.getPromotionType() != null &&
            order.getPromotionType() == 3 &&
            order.getGroupStatus() != null &&
            order.getGroupStatus() == 0
        );
        List<OrderDetailDTO> result = new ArrayList<>();
        for (Order order : orders) {
            result.add(buildOrderDetailDTO(order));
        }
        return result;
    }

    // 构建订单详情DTO
    private OrderDetailDTO buildOrderDetailDTO(Order order) {
        OrderDetailDTO dto = new OrderDetailDTO();
        BeanUtils.copyProperties(order, dto);

        // 设置订单状态文本
        String[] statusTexts = {"待付款", "待发货", "待收货", "已完成", "已取消"};
        dto.setStatusText(order.getStatus() != null && order.getStatus() < statusTexts.length ?
                statusTexts[order.getStatus()] : "未知状态");

        // 设置售后状态文本
        String[] afterSaleStatusTexts = {"", "待商家确认", "商家同意退货", "买家已发货", "退款成功", "商家拒绝退货"};
        dto.setAfterSaleStatusText(order.getAfterSaleStatus() != null && order.getAfterSaleStatus() < afterSaleStatusTexts.length ?
                afterSaleStatusTexts[order.getAfterSaleStatus()] : "");

        // 查询商品信息
        Goods goods = goodsMapper.findById(order.getGoodsId());
        if (goods != null) {
            dto.setGoodsName(goods.getTitle());
            dto.setGoodsDescription(goods.getDescription());
            dto.setGoodsImage(goods.getImageUrl());
            dto.setGoodsPrice(goods.getPrice());
        }

        // 查询买家信息
        User buyer = userMapper.findById(order.getBuyerId());
        if (buyer != null) {
            dto.setBuyerUsername(buyer.getUsername());
            dto.setBuyerNickname(buyer.getNickname());
            dto.setBuyerAvatar(buyer.getAvatar());
        }

        // 查询卖家信息
        User seller = userMapper.findById(order.getSellerId());
        if (seller != null) {
            dto.setSellerUsername(seller.getUsername());
            dto.setSellerNickname(seller.getNickname());
            dto.setSellerAvatar(seller.getAvatar());
        }

        // 查询地址信息
        if (order.getAddressId() != null) {
            Address address = addressMapper.findById(order.getAddressId());
            if (address != null) {
                dto.setAddressName(address.getName());
                dto.setAddressPhone(address.getPhone());
                dto.setAddressProvince(address.getProvince());
                dto.setAddressCity(address.getCity());
                dto.setAddressDistrict(address.getDistrict());
                dto.setAddressDetail(address.getDetail());
            }
        }

        return dto;
    }

    @Override
    public void updateStatus(java.lang.Long id, Integer status) {
        orderMapper.updateStatus(id, status);
    }

    @Override
    public void updateGroupStatus(java.lang.Long id, Integer groupStatus) {
        orderMapper.updateGroupStatus(id, groupStatus);
    }

    @Override
    public Order getById(java.lang.Long id) {
        return orderMapper.findById(id);
    }

    @Override
    public void applyRefund(java.lang.Long orderId, java.lang.Long buyerId, String remark) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (order.getStatus() != 1 && order.getStatus() != 2) {
            throw new RuntimeException("订单状态不正确，无法申请退货");
        }
        if (order.getAfterSaleStatus() != null && order.getAfterSaleStatus() != 0 && order.getAfterSaleStatus() != 5) {
            throw new RuntimeException("已有售后申请处理中");
        }
        orderMapper.updateAfterSale(orderId, 1, remark);
    }

    @Override
    public void approveRefund(java.lang.Long orderId, java.lang.Long sellerId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getSellerId().equals(sellerId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (order.getAfterSaleStatus() != 1) {
            throw new RuntimeException("售后状态不正确，无法同意退货");
        }
        orderMapper.updateAfterSaleStatus(orderId, 2);
    }

    @Override
    public void rejectRefund(java.lang.Long orderId, java.lang.Long sellerId, String remark) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getSellerId().equals(sellerId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (order.getAfterSaleStatus() != 1 && order.getAfterSaleStatus() != 3) {
            throw new RuntimeException("售后状态不正确，无法拒绝退款");
        }
        orderMapper.updateAfterSale(orderId, 5, remark);
    }

    @Override
    public void confirmShipRefund(java.lang.Long orderId, java.lang.Long buyerId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (order.getAfterSaleStatus() != 2) {
            throw new RuntimeException("售后状态不正确，无法确认发货");
        }
        orderMapper.updateAfterSaleStatus(orderId, 3);
    }

    @Override
    public void confirmReceiveRefund(java.lang.Long orderId, java.lang.Long sellerId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getSellerId().equals(sellerId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (order.getAfterSaleStatus() != 3) {
            throw new RuntimeException("售后状态不正确，无法确认收货");
        }
        orderMapper.updateAfterSaleStatus(orderId, 4);
        orderMapper.updateStatus(orderId, 3);
        goodsService.increaseStock(order.getGoodsId(), 1);
    }

    @Override
    public void cancelRefund(java.lang.Long orderId, java.lang.Long buyerId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (order.getAfterSaleStatus() != 1) {
            throw new RuntimeException("当前状态无法取消退货");
        }
        orderMapper.updateAfterSale(orderId, 0, null);
    }
    
    @Override
    public List<OrderDetailDTO> listAll() {
        List<Order> orders = orderMapper.listAll();
        List<OrderDetailDTO> result = new ArrayList<>();

        for (Order order : orders) {
            OrderDetailDTO dto = new OrderDetailDTO();
            BeanUtils.copyProperties(order, dto);

            // 处理状态文本
            dto.setStatusText(getStatusText(order.getStatus()));
            dto.setAfterSaleStatusText(getAfterSaleStatusText(order.getAfterSaleStatus()));

            // 获取商品信息
            Goods goods = goodsMapper.findById(order.getGoodsId());
            if (goods != null) {
                dto.setGoodsName(goods.getTitle());
                dto.setGoodsDescription(goods.getDescription());
                dto.setGoodsImage(goods.getImageUrl());
                dto.setGoodsPrice(goods.getPrice());
            }

            // 获取买家信息
            User buyer = userMapper.findById(order.getBuyerId());
            if (buyer != null) {
                dto.setBuyerUsername(buyer.getUsername());
                dto.setBuyerNickname(buyer.getNickname());
                dto.setBuyerAvatar(buyer.getAvatar());
            }

            // 获取卖家信息
            User seller = userMapper.findById(order.getSellerId());
            if (seller != null) {
                dto.setSellerUsername(seller.getUsername());
                dto.setSellerNickname(seller.getNickname());
                dto.setSellerAvatar(seller.getAvatar());
            }

            // 查询地址信息
            if (order.getAddressId() != null) {
                Address address = addressMapper.findById(order.getAddressId());
                if (address != null) {
                    dto.setAddressName(address.getName());
                    dto.setAddressPhone(address.getPhone());
                    dto.setAddressProvince(address.getProvince());
                    dto.setAddressCity(address.getCity());
                    dto.setAddressDistrict(address.getDistrict());
                    dto.setAddressDetail(address.getDetail());
                }
            }

            result.add(dto);
        }

        return result;
    }
    
    private String getStatusText(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "待付款";
            case 1: return "待发货";
            case 2: return "待收货";
            case 3: return "已完成";
            case 4: return "已取消";
            default: return "未知";
        }
    }
    
    private String getAfterSaleStatusText(Integer status) {
        if (status == null || status == 0) return "";
        switch (status) {
            case 1: return "待商家确认";
            case 2: return "商家同意退货";
            case 3: return "买家已发货";
            case 4: return "退款成功";
            case 5: return "商家拒绝退货";
            default: return "";
        }
    }
}

