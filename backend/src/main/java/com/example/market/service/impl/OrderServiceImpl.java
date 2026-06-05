package com.example.market.service.impl;

import com.example.market.dto.OrderDetailDTO;
import com.example.market.entity.Goods;
import com.example.market.entity.Order;
import com.example.market.entity.User;
import com.example.market.mapper.GoodsMapper;
import com.example.market.mapper.OrderMapper;
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
    private GoodsService goodsService;

    @Override
    public Order createOrder(java.lang.Long goodsId, java.lang.Long buyerId) {
        Goods goods = goodsMapper.findById(goodsId);
        if (goods == null || goods.getStatus() != 1) {
            throw new RuntimeException("商品不存在或未上架");
        }
        if (goods.getSellerId().equals(buyerId)) {
            throw new RuntimeException("不能购买自己的商品");
        }
        if (!goodsService.decreaseStock(goodsId, 1)) {
            throw new RuntimeException("库存不足");
        }
        Order order = new Order();
        order.setGoodsId(goodsId);
        order.setBuyerId(buyerId);
        order.setSellerId(goods.getSellerId());
        BigDecimal price = goods.getPrice() == null ? BigDecimal.ZERO : goods.getPrice();
        BigDecimal freight = goods.getFreight() == null ? BigDecimal.ZERO : goods.getFreight();
        order.setAmount(price.add(freight));
        order.setStatus(0);
        orderMapper.insert(order);
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
    }

    @Override
    public List<Order> listByBuyer(java.lang.Long buyerId) {
        return orderMapper.listByBuyer(buyerId);
    }

    @Override
    public List<Order> listBySeller(java.lang.Long sellerId) {
        return orderMapper.listBySeller(sellerId);
    }

    @Override
    public void updateStatus(java.lang.Long id, Integer status) {
        orderMapper.updateStatus(id, status);
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

