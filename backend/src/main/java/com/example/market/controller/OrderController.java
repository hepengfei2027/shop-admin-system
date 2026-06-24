package com.example.market.controller;

import com.example.market.dto.OrderDetailDTO;
import com.example.market.dto.Result;
import com.example.market.entity.Order;
import com.example.market.service.OrderService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/order")
@CrossOrigin
public class OrderController {

    @Resource
    private OrderService orderService;

    @PostMapping("/create")
    public Result<Order> create(@RequestParam java.lang.Long goodsId,
                                @RequestParam java.lang.Long buyerId,
                                @RequestParam(required = false) java.lang.Long addressId,
                                @RequestParam(required = false, defaultValue = "1") Integer quantity,
                                @RequestParam(required = false) java.lang.Long couponId,
                                @RequestParam(required = false) Integer promotionType,
                                @RequestParam(required = false) java.lang.Long promotionId,
                                @RequestParam(required = false) java.math.BigDecimal promotionDiscount) {
        return Result.ok(orderService.createOrder(goodsId, buyerId, addressId, quantity, couponId, promotionType, promotionId, promotionDiscount));
    }

    @GetMapping("/buyer/{buyerId}")
    public Result<List<Order>> listByBuyer(@PathVariable java.lang.Long buyerId) {
        return Result.ok(orderService.listByBuyer(buyerId));
    }

    @GetMapping("/seller/{sellerId}")
    public Result<List<Order>> listBySeller(@PathVariable java.lang.Long sellerId) {
        return Result.ok(orderService.listBySeller(sellerId));
    }

    @GetMapping("/buyer/{buyerId}/detail")
    public Result<List<OrderDetailDTO>> listByBuyerWithDetails(@PathVariable java.lang.Long buyerId) {
        return Result.ok(orderService.listByBuyerWithDetails(buyerId));
    }

    @GetMapping("/seller/{sellerId}/detail")
    public Result<List<OrderDetailDTO>> listBySellerWithDetails(@PathVariable java.lang.Long sellerId) {
        return Result.ok(orderService.listBySellerWithDetails(sellerId));
    }

    @PostMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable java.lang.Long id,
                                     @RequestParam Integer status) {
        orderService.updateStatus(id, status);
        return Result.ok(null);
    }

    @GetMapping("/my")
    public Result<List<Order>> myOrders(@RequestParam java.lang.Long userId) {
        List<Order> list = orderService.listByBuyer(userId);
        return Result.ok(list);
    }

    @PostMapping("/{id}/pay")
    public Result<Void> payOrder(@PathVariable java.lang.Long id,
                                  @RequestParam java.lang.Long buyerId) {
        orderService.payOrder(id, buyerId);
        return Result.ok(null);
    }

    /**
     * 余额支付
     * @param id 订单ID
     * @param buyerId 买家ID
     * @param paymentMethod 支付方式：balance/wechat/alipay
     */
    @PostMapping("/{id}/payV2")
    public Result<Void> payOrderV2(@PathVariable java.lang.Long id,
                                    @RequestParam java.lang.Long buyerId,
                                    @RequestParam(required = false, defaultValue = "wechat") String paymentMethod) {
        orderService.payOrderV2(id, buyerId, paymentMethod);
        return Result.ok(null);
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable java.lang.Long id,
                                      @RequestParam java.lang.Long userId) {
        orderService.cancelOrder(id, userId);
        return Result.ok(null);
    }

    @PostMapping("/{id}/ship")
    public Result<Void> shipOrder(@PathVariable java.lang.Long id,
                                    @RequestParam java.lang.Long sellerId) {
        orderService.shipOrder(id, sellerId);
        return Result.ok(null);
    }

    @PostMapping("/{id}/confirm")
    public Result<Void> confirmReceive(@PathVariable java.lang.Long id,
                                        @RequestParam java.lang.Long buyerId) {
        orderService.confirmReceive(id, buyerId);
        return Result.ok(null);
    }

    @GetMapping("/info/{id}")
    public Result<Order> getOrderInfo(@PathVariable java.lang.Long id) {
        return Result.ok(orderService.getById(id));
    }

    @PostMapping("/{id}/refund/apply")
    public Result<Void> applyRefund(@PathVariable java.lang.Long id,
                                     @RequestParam java.lang.Long buyerId,
                                     @RequestParam(required = false) String remark) {
        orderService.applyRefund(id, buyerId, remark);
        return Result.ok(null);
    }

    @PostMapping("/{id}/refund/approve")
    public Result<Void> approveRefund(@PathVariable java.lang.Long id,
                                       @RequestParam java.lang.Long sellerId) {
        orderService.approveRefund(id, sellerId);
        return Result.ok(null);
    }

    @PostMapping("/{id}/refund/reject")
    public Result<Void> rejectRefund(@PathVariable java.lang.Long id,
                                       @RequestParam java.lang.Long sellerId,
                                       @RequestParam(required = false) String remark) {
        orderService.rejectRefund(id, sellerId, remark);
        return Result.ok(null);
    }

    @PostMapping("/{id}/refund/ship")
    public Result<Void> confirmShipRefund(@PathVariable java.lang.Long id,
                                           @RequestParam java.lang.Long buyerId) {
        orderService.confirmShipRefund(id, buyerId);
        return Result.ok(null);
    }

    @PostMapping("/{id}/refund/confirm")
    public Result<Void> confirmReceiveRefund(@PathVariable java.lang.Long id,
                                              @RequestParam java.lang.Long sellerId) {
        orderService.confirmReceiveRefund(id, sellerId);
        return Result.ok(null);
    }

    @PostMapping("/{id}/refund/cancel")
    public Result<Void> cancelRefund(@PathVariable java.lang.Long id,
                                     @RequestParam java.lang.Long buyerId) {
        orderService.cancelRefund(id, buyerId);
        return Result.ok(null);
    }
    
    @GetMapping("/all")
    public Result<List<OrderDetailDTO>> getAllOrders() {
        return Result.ok(orderService.listAll());
    }
}

