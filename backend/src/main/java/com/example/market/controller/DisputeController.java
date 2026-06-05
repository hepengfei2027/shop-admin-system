package com.example.market.controller;

import com.example.market.dto.Result;
import com.example.market.entity.Dispute;
import com.example.market.service.DisputeService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/dispute")
@CrossOrigin
public class DisputeController {

    @Resource
    private DisputeService disputeService;

    @PostMapping("/create")
    public Result<Dispute> create(@RequestParam Long orderId,
                                   @RequestParam Long buyerId,
                                   @RequestParam(required = false) String buyerContent,
                                   @RequestParam(required = false) String buyerImages) {
        return Result.ok(disputeService.createDispute(orderId, buyerId, buyerContent, buyerImages));
    }

    @GetMapping("/detail/{id}")
    public Result<Dispute> getDetail(@PathVariable Long id) {
        return Result.ok(disputeService.getDisputeById(id));
    }

    @GetMapping("/order/{orderId}")
    public Result<Dispute> getByOrderId(@PathVariable Long orderId) {
        return Result.ok(disputeService.getDisputeByOrderId(orderId));
    }

    @GetMapping("/list")
    public Result<List<Dispute>> list() {
        return Result.ok(disputeService.getAllDisputes());
    }

    @PostMapping("/seller/reply")
    public Result<Void> sellerReply(@RequestParam Long id,
                                    @RequestParam(required = false) String sellerReply,
                                    @RequestParam(required = false) String sellerImages) {
        disputeService.sellerReply(id, sellerReply, sellerImages);
        return Result.ok(null);
    }

    @PostMapping("/admin/decision")
    public Result<Void> adminDecision(@RequestParam Long id,
                                       @RequestParam String adminDecision,
                                       @RequestParam(required = false) String adminRemark,
                                       @RequestParam Long adminId,
                                       @RequestParam Integer status) {
        disputeService.adminDecision(id, adminDecision, adminRemark, adminId, status);
        return Result.ok(null);
    }
}
