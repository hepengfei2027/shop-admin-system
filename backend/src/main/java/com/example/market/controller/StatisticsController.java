package com.example.market.controller;

import com.example.market.dto.Result;
import com.example.market.entity.Goods;
import com.example.market.entity.Order;
import com.example.market.entity.User;
import com.example.market.mapper.GoodsMapper;
import com.example.market.mapper.OrderMapper;
import com.example.market.mapper.UserMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
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
        List<User> allUsers = userMapper.findAll();
        List<Goods> allGoods = goodsMapper.findAll();
        
        // 总营收（已完成订单）
        double totalRevenue = allOrders.stream()
                .filter(o -> o.getStatus() == 3)
                .mapToDouble(o -> o.getAmount() != null ? o.getAmount().doubleValue() : 0)
                .sum();
        
        // 总订单数
        long totalOrders = allOrders.size();
        
        // 已完成订单数
        long completedOrders = allOrders.stream()
                .filter(o -> o.getStatus() == 3)
                .count();
        
        // 待处理订单数（待付款、待发货）
        long pendingOrders = allOrders.stream()
                .filter(o -> o.getStatus() == 1 || o.getStatus() == 2)
                .count();
        
        // 封禁用户数
        long bannedUsers = allUsers.stream()
                .filter(u -> u.getStatus() != null && u.getStatus() == 2)
                .count();
        
        // 活跃卖家数（有上架商品且状态正常的卖家）
        Set<Long> activeSellerIds = allGoods.stream()
                .filter(g -> g.getStatus() != null && g.getStatus() == 1)
                .map(Goods::getSellerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
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
        data.put("completedOrders", completedOrders);
        data.put("pendingOrders", pendingOrders);
        data.put("bannedUsers", bannedUsers);
        data.put("activeSellers", activeSellerIds.size());
        data.put("totalRefunds", totalRefunds);
        data.put("refundRate", refundRate);
        data.put("totalUsers", totalUsers);
        data.put("totalGoods", totalGoods);
        data.put("popularGoods", popularGoods);
        
        return Result.ok(data);
    }

    // 全平台销售趋势
    @GetMapping("/platform/trend")
    public Result<List<Map<String, Object>>> getPlatformTrend() {
        List<Order> allOrders = orderMapper.findAll();
        Map<String, List<Order>> ordersByDate = allOrders.stream()
                .filter(o -> o.getStatus() == 3 && o.getCreateTime() != null)
                .collect(Collectors.groupingBy(o -> {
                    String time = o.getCreateTime().toString();
                    return time.substring(5, 10); // MM-dd格式
                }));

        List<Map<String, Object>> trend = new ArrayList<>();
        // 近7天数据
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_MONTH, -i);
            String dateKey = sdf.format(cal.getTime());
            
            List<Order> dayOrders = ordersByDate.getOrDefault(dateKey, Collections.emptyList());
            double amount = dayOrders.stream()
                    .mapToDouble(o -> o.getAmount() != null ? o.getAmount().doubleValue() : 0)
                    .sum();
            
            Map<String, Object> item = new HashMap<>();
            item.put("date", dateKey);
            item.put("amount", amount);
            item.put("orders", dayOrders.size());
            trend.add(item);
        }
        
        return Result.ok(trend);
    }

    // 全平台热销商品
    @GetMapping("/platform/top-goods")
    public Result<List<Map<String, Object>>> getTopGoods() {
        List<Order> completedOrders = orderMapper.findAll().stream()
                .filter(o -> o.getStatus() == 3)
                .collect(Collectors.toList());
        
        // 按商品分组计算销量和销售额
        Map<Long, Map<String, Object>> goodsStats = new HashMap<>();
        for (Order order : completedOrders) {
            Goods goods = goodsMapper.findById(order.getGoodsId());
            if (goods == null) continue;
            
            Map<String, Object> stats = goodsStats.computeIfAbsent(order.getGoodsId(), k -> {
                Map<String, Object> s = new HashMap<>();
                s.put("goodsId", order.getGoodsId());
                s.put("title", goods.getTitle());
                s.put("salesCount", 0L);
                s.put("revenue", 0.0);
                return s;
            });
            
            stats.put("salesCount", (Long) stats.get("salesCount") + 1);
            stats.put("revenue", (Double) stats.get("revenue") + 
                    (order.getAmount() != null ? order.getAmount().doubleValue() : 0));
        }
        
        List<Map<String, Object>> topGoods = new ArrayList<>(goodsStats.values());
        topGoods.sort((a, b) -> Long.compare((Long) b.get("salesCount"), (Long) a.get("salesCount")));
        
        return Result.ok(topGoods.stream().limit(10).collect(Collectors.toList()));
    }

    // 用户分析
    @GetMapping("/platform/user-analysis")
    public Result<Map<String, Object>> getUserAnalysis() {
        List<User> allUsers = userMapper.findAll();
        List<Order> allOrders = orderMapper.findAll();
        
        // 买家统计（有过购买行为的用户）
        Set<Long> buyerIds = allOrders.stream()
                .map(Order::getBuyerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        // 卖家统计（有发布过商品的用户）
        Set<Long> sellerIds = goodsMapper.findAll().stream()
                .map(Goods::getSellerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        // 活跃用户（有过订单的用户）
        Set<Long> activeUserIds = new HashSet<>();
        activeUserIds.addAll(allOrders.stream().map(Order::getBuyerId).filter(Objects::nonNull).collect(Collectors.toSet()));
        activeUserIds.addAll(allOrders.stream().map(Order::getSellerId).filter(Objects::nonNull).collect(Collectors.toSet()));
        
        // 高消费用户排行
        Map<Long, Map<String, Object>> userSpendMap = new HashMap<>();
        for (Order order : allOrders) {
            if (order.getStatus() == 3 && order.getBuyerId() != null) {
                Map<String, Object> userStats = userSpendMap.computeIfAbsent(order.getBuyerId(), k -> {
                    Map<String, Object> s = new HashMap<>();
                    s.put("id", order.getBuyerId());
                    s.put("totalAmount", 0.0);
                    s.put("orderCount", 0L);
                    return s;
                });
                userStats.put("totalAmount", (Double) userStats.get("totalAmount") + 
                        (order.getAmount() != null ? order.getAmount().doubleValue() : 0));
                userStats.put("orderCount", (Long) userStats.get("orderCount") + 1);
            }
        }
        
        // 添加用户名信息
        for (Map<String, Object> stats : userSpendMap.values()) {
            Long userId = ((Number) stats.get("id")).longValue();
            User user = userMapper.findById(userId);
            if (user != null) {
                stats.put("username", user.getUsername());
                stats.put("nickname", user.getNickname());
            }
        }
        
        List<Map<String, Object>> topSpending = new ArrayList<>(userSpendMap.values());
        topSpending.sort((a, b) -> Double.compare((Double) b.get("totalAmount"), (Double) a.get("totalAmount")));
        
        // 活跃用户排行（按订单数）
        List<Map<String, Object>> topActive = new ArrayList<>(userSpendMap.values());
        topActive.sort((a, b) -> Long.compare((Long) b.get("orderCount"), (Long) a.get("orderCount")));
        
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("totalBuyers", buyerIds.size());
        analysis.put("totalSellers", sellerIds.size());
        analysis.put("newUsersToday", 0); // 需要更复杂的逻辑来计算
        analysis.put("activeUsers", activeUserIds.size());
        analysis.put("topSpendingUsers", topSpending.stream().limit(5).collect(Collectors.toList()));
        analysis.put("topActiveUsers", topActive.stream().limit(5).collect(Collectors.toList()));
        
        return Result.ok(analysis);
    }

    // 订单分析
    @GetMapping("/platform/order-analysis")
    public Result<Map<String, Object>> getOrderAnalysis() {
        List<Order> allOrders = orderMapper.findAll();
        List<Order> completedOrders = allOrders.stream()
                .filter(o -> o.getStatus() == 3)
                .collect(Collectors.toList());
        
        // 平均客单价
        double avgOrderValue = completedOrders.stream()
                .mapToDouble(o -> o.getAmount() != null ? o.getAmount().doubleValue() : 0)
                .average()
                .orElse(0);
        
        // 退款率
        long refundCount = allOrders.stream()
                .filter(o -> o.getAfterSaleStatus() != null && o.getAfterSaleStatus() > 0)
                .count();
        double refundRate = allOrders.size() > 0 ? (refundCount * 100.0 / allOrders.size()) : 0;
        
        // 完成率
        long completedCount = completedOrders.size();
        double completionRate = allOrders.size() > 0 ? (completedCount * 100.0 / allOrders.size()) : 0;
        
        // 待处理订单
        long pendingOrders = allOrders.stream()
                .filter(o -> o.getStatus() == 1 || o.getStatus() == 2)
                .count();
        
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("avgOrderValue", avgOrderValue);
        analysis.put("refundRate", refundRate);
        analysis.put("completionRate", completionRate);
        analysis.put("pendingOrders", pendingOrders);
        
        return Result.ok(analysis);
    }
}
