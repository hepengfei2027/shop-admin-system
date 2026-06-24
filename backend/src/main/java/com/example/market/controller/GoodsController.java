package com.example.market.controller;

import com.example.market.dto.GoodsWithPromotionDTO;
import com.example.market.dto.Result;
import com.example.market.entity.Goods;
import com.example.market.entity.PromotionActivity;
import com.example.market.entity.PromotionDiscount;
import com.example.market.entity.PromotionFullReduce;
import com.example.market.entity.PromotionGroup;
import com.example.market.mapper.PromotionActivityMapper;
import com.example.market.mapper.PromotionDiscountMapper;
import com.example.market.mapper.PromotionFullReduceMapper;
import com.example.market.mapper.PromotionGroupMapper;
import com.example.market.service.GoodsService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/goods")
@CrossOrigin
public class GoodsController {

    @Resource
    private GoodsService goodsService;

    @Resource
    private PromotionActivityMapper promotionActivityMapper;

    @Resource
    private PromotionDiscountMapper promotionDiscountMapper;

    @Resource
    private PromotionFullReduceMapper promotionFullReduceMapper;

    @Resource
    private PromotionGroupMapper promotionGroupMapper;

    @GetMapping("/list")
    public Result<List<GoodsWithPromotionDTO>> list() {
        List<Goods> goodsList = goodsService.listOnSale();
        return Result.ok(addPromotionInfo(goodsList));
    }

    // 管理员获取所有商品（包括已下架、审核中等）
    @GetMapping("/all")
    public Result<List<Goods>> listAll() {
        List<Goods> goodsList = goodsService.listAll();
        return Result.ok(goodsList);
    }

    @GetMapping("/search")
    public Result<List<GoodsWithPromotionDTO>> search(@RequestParam String keyword) {
        List<Goods> goodsList = goodsService.search(keyword);
        return Result.ok(addPromotionInfo(goodsList));
    }

    @GetMapping("/search/filter")
    public Result<List<GoodsWithPromotionDTO>> searchWithFilter(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice) {
        List<Goods> goodsList = goodsService.searchWithFilter(keyword, brand, minPrice, maxPrice);
        return Result.ok(addPromotionInfo(goodsList));
    }

    @GetMapping("/brands")
    public Result<List<Goods>> listBrands() {
        return Result.ok(goodsService.listBrands());
    }

    @PostMapping("/publish")
    public Result<Goods> publish(@RequestBody Goods goods) {
        if (goods.getStatus() == null) {
            goods.setStatus(0);
        }
        Goods g = goodsService.publish(goods);
        return Result.ok(g);
    }

    @GetMapping("/my")
    public Result<List<Goods>> listMyGoods(@RequestParam java.lang.Long userId) {
        List<Goods> list = goodsService.listMyGoods(userId);
        return Result.ok(list);
    }

    @GetMapping("/pending")
    public Result<List<Goods>> listPending() {
        List<Goods> list = goodsService.listPending();
        return Result.ok(list);
    }

    @PostMapping("/approve/{id}")
    public Result<Void> approve(@PathVariable java.lang.Long id) {
        goodsService.approve(id);
        return Result.ok(null);
    }

    @PostMapping("/reject/{id}")
    public Result<Void> reject(@PathVariable java.lang.Long id) {
        goodsService.reject(id);
        return Result.ok(null);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable java.lang.Long id) {
        goodsService.delete(id);
        return Result.ok(null);
    }

    @PostMapping("/updateStatus/{id}")
    public Result<Void> updateStatus(@PathVariable java.lang.Long id, @RequestParam Integer status) {
        goodsService.updateStatus(id, status);
        return Result.ok(null);
    }

    @GetMapping("/detail/{id}")
    public Result<Goods> detail(@PathVariable java.lang.Long id) {
        Goods goods = goodsService.findById(id);
        return Result.ok(goods);
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestBody Goods goods) {
        goodsService.update(goods);
        return Result.ok(null);
    }

    private List<GoodsWithPromotionDTO> addPromotionInfo(List<Goods> goodsList) {
        List<GoodsWithPromotionDTO> result = new ArrayList<>();
        for (Goods goods : goodsList) {
            GoodsWithPromotionDTO dto = GoodsWithPromotionDTO.fromGoods(goods);
            
            List<PromotionActivity> activities = promotionActivityMapper.findActiveByGoodsId(goods.getId());
            
            BigDecimal bestPrice = goods.getPrice();
            Integer bestType = null;
            String bestLabel = null;
            BigDecimal promotionDiscount = BigDecimal.ZERO;
            
            for (PromotionActivity activity : activities) {
                if (activity.getType() == 2) {
                    PromotionDiscount discount = promotionDiscountMapper.findByActivityId(activity.getId());
                    if (discount != null && discount.getDiscountRate() != null) {
                        BigDecimal discountedPrice = goods.getPrice().multiply(discount.getDiscountRate());
                        if (discountedPrice.compareTo(bestPrice) < 0) {
                            bestPrice = discountedPrice;
                            bestType = 2;
                            bestLabel = "限时折扣";
                            promotionDiscount = goods.getPrice().subtract(discountedPrice);
                        }
                        // 标记有折扣活动
                        dto.setHasDiscount(true);
                        dto.setDiscountRate(discount.getDiscountRate());
                    }
                } else if (activity.getType() == 3) {
                    PromotionGroup group = promotionGroupMapper.findByActivityId(activity.getId());
                    if (group != null && group.getGroupPrice() != null) {
                        if (group.getGroupPrice().compareTo(bestPrice) < 0) {
                            bestPrice = group.getGroupPrice();
                            bestType = 3;
                            bestLabel = "团购";
                            promotionDiscount = goods.getPrice().subtract(group.getGroupPrice());
                        }
                    }
                } else if (activity.getType() == 1) {
                    PromotionFullReduce fullReduce = promotionFullReduceMapper.findByActivityId(activity.getId());
                    if (fullReduce != null && fullReduce.getThresholdAmount() != null && fullReduce.getReductionAmount() != null) {
                        // 满减活动单独标记，不影响最佳价格类型
                        dto.setHasFullReduce(true);
                        dto.setFullReduceThreshold(fullReduce.getThresholdAmount());
                        dto.setFullReduceAmount(fullReduce.getReductionAmount());
                        
                        // 如果没有更优惠的折扣或团购，才设置满减为最佳类型
                        if (bestType == null || bestType == 1) {
                            bestType = 1;
                            bestLabel = "满减优惠";
                        }
                    }
                }
            }
            
            dto.setPromotionalPrice(bestPrice);
            dto.setPromotionType(bestType);
            dto.setPromotionLabel(bestLabel);
            dto.setPromotionDiscount(promotionDiscount);
            result.add(dto);
        }
        return result;
    }
}

