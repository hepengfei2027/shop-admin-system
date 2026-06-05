package com.example.market.controller;

import com.example.market.dto.Result;
import com.example.market.entity.Goods;
import com.example.market.service.GoodsService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/goods")
@CrossOrigin
public class GoodsController {

    @Resource
    private GoodsService goodsService;

    @GetMapping("/list")
    public Result<List<Goods>> list() {
        return Result.ok(goodsService.listOnSale());
    }

    @GetMapping("/search")
    public Result<List<Goods>> search(@RequestParam String keyword) {
        return Result.ok(goodsService.search(keyword));
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
}

