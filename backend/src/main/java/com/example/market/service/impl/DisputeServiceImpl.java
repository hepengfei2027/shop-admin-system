package com.example.market.service.impl;

import com.example.market.entity.Dispute;
import com.example.market.entity.Order;
import com.example.market.mapper.DisputeMapper;
import com.example.market.mapper.OrderMapper;
import com.example.market.service.DisputeService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class DisputeServiceImpl implements DisputeService {

    @Resource
    private DisputeMapper disputeMapper;

    @Resource
    private OrderMapper orderMapper;

    @Override
    public Dispute createDispute(Long orderId, Long buyerId, String buyerContent, String buyerImages) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (order.getAfterSaleStatus() != 5) {
            throw new RuntimeException("只有商家拒绝退货的订单才能申请平台介入");
        }

        Dispute dispute = new Dispute();
        dispute.setOrderId(orderId);
        dispute.setBuyerId(buyerId);
        dispute.setSellerId(order.getSellerId());
        dispute.setStatus(1); // 买家申请平台介入
        dispute.setBuyerContent(buyerContent);
        dispute.setBuyerImages(buyerImages);
        disputeMapper.insert(dispute);
        return dispute;
    }

    @Override
    public Dispute getDisputeById(Long id) {
        return disputeMapper.findById(id);
    }

    @Override
    public Dispute getDisputeByOrderId(Long orderId) {
        return disputeMapper.findByOrderId(orderId);
    }

    @Override
    public List<Dispute> getAllDisputes() {
        return disputeMapper.findAll();
    }

    @Override
    public void sellerReply(Long id, String sellerReply, String sellerImages) {
        Dispute dispute = disputeMapper.findById(id);
        if (dispute == null) {
            throw new RuntimeException("纠纷不存在");
        }
        
        disputeMapper.updateSellerReply(id, sellerReply, sellerImages);
        
        if (dispute.getStatus() == 1) {
            disputeMapper.updateStatus(id, 2);
        }
    }

    @Override
    public void adminDecision(Long id, String adminDecision, String adminRemark, Long adminId, Integer status) {
        Dispute dispute = disputeMapper.findById(id);
        if (dispute == null) {
            throw new RuntimeException("纠纷不存在");
        }
        
        disputeMapper.updateAdminDecision(id, adminDecision, adminRemark, adminId, status);
        
        Order order = orderMapper.findById(dispute.getOrderId());
        if (order != null) {
            if (status == 3) {
                orderMapper.updateAfterSaleStatus(order.getId(), 2);
            } else if (status == 4) {
                orderMapper.updateAfterSaleStatus(order.getId(), 4);
            }
        }
    }
}
