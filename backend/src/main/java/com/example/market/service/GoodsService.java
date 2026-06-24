package com.example.market.service;

import com.example.market.entity.Goods;

import java.util.List;

public interface GoodsService {

    Goods publish(Goods goods);

    List<Goods> listOnSale();

    List<Goods> listAll(); // 管理员获取所有商品

    List<Goods> search(String keyword);

    List<Goods> listBrands();

    List<Goods> searchWithFilter(String keyword, String brand, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice);

    Goods findById(Long id);

    void updateStatus(Long id, Integer status);
    List<Goods> listMyGoods(Long userId);
    List<Goods> listPending();
    void approve(Long id);
    void reject(Long id);
    void delete(Long id);

    boolean decreaseStock(Long id, Integer quantity);

    void increaseStock(Long id, Integer quantity);

    void update(Goods goods);
}

