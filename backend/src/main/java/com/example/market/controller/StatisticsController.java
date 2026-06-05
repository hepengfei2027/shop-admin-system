package com.example.market.controller;

import com.example.market.dto.Result;
import com.example.market.entity.Goods;
import com.example.market.entity.Order;
import com.example.market.mapper.GoodsMapper;
import com.example.market.mapper.OrderMapper;
import com.example.market.mapper.UserMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/statistics")
@CrossOrigin
public class StatisticsController {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private GoodsMapper goodsMapper;

    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        Map<String, Object> data = new HashMap<>();
        
        List<Order> allOrders = orderMapper.findAll();
        
        double totalRevenue = allOrders.stream()
                .filter(o -> o.getStatus() == 3)
                .mapToDouble(o -> o.getAmount() != null ? o.getAmount().doubleValue() : 0)
                .sum();
        
        long totalOrders = allOrders.stream()
                .filter(o -> o.getStatus() == 3)
                .count();
        
        long totalRefunds = allOrders.stream()
                .filter(o -> o.getAfterSaleStatus() != null && o.getAfterSaleStatus() > 0)
                .count();
        
        double refundRate = totalOrders > 0 ? (totalRefunds * 100.0 / totalOrders) : 0;
        
        long totalUsers = userMapper.count();
        
        long totalGoods = goodsMapper.count();
        
        // 计算每个商品的销量
        Map<Long, Long> goodsSalesMap = allOrders.stream()
                .filter(o -> o.getStatus() == 3) // 只统计已完成的订单
                .collect(Collectors.groupingBy(Order::getGoodsId, Collectors.counting()));

        List<Goods> allGoods = goodsMapper.findAll();
        List<Map<String, Object>> popularGoods = allGoods.stream()
                .map(g -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", g.getId());
                    item.put("name", g.getTitle());
                    item.put("image", g.getImageUrl());
                    item.put("price", g.getPrice() != null ? g.getPrice().doubleValue() : 0);
                    item.put("sales", goodsSalesMap.getOrDefault(g.getId(), 0L));
                    item.put("status", g.getStatus());
                    return item;
                })
                .filter(item -> (Long) item.get("sales") > 0)
                .sorted(Comparator.comparing((Map<String, Object> item) -> (Long) item.get("sales")).reversed())
                .limit(10)
                .collect(Collectors.toList());
        
        data.put("totalRevenue", totalRevenue);
        data.put("totalOrders", totalOrders);
        data.put("totalRefunds", totalRefunds);
        data.put("refundRate", refundRate);
        data.put("totalUsers", totalUsers);
        data.put("totalGoods", totalGoods);
        data.put("popularGoods", popularGoods);
        
        return Result.ok(data);
    }
}
