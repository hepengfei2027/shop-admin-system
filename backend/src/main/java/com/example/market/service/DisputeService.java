package com.example.market.service;

import com.example.market.entity.Dispute;

import java.util.List;

public interface DisputeService {

    Dispute createDispute(Long orderId, Long buyerId, String buyerContent, String buyerImages);

    Dispute getDisputeById(Long id);

    Dispute getDisputeByOrderId(Long orderId);

    List<Dispute> getAllDisputes();

    void sellerReply(Long id, String sellerReply, String sellerImages);

    void adminDecision(Long id, String adminDecision, String adminRemark, Long adminId, Integer status);
}
