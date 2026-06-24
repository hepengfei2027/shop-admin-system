package com.example.market.service.impl;

import com.example.market.entity.Goods;
import com.example.market.mapper.GoodsMapper;
import com.example.market.service.GoodsService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class GoodsServiceImpl implements GoodsService {

    @Resource
    private GoodsMapper goodsMapper;

    @Override
    public Goods publish(Goods goods) {
        if (goods.getId() != null) {
            update(goods);
            return goodsMapper.findById(goods.getId());
        }
        if (goods.getStatus() == null) {
            goods.setStatus(0);
        }
        goodsMapper.insert(goods);
        return goods;
    }

    @Override
    public List<Goods> listOnSale() {
        return goodsMapper.listOnSale();
    }

    @Override
    public List<Goods> listAll() {
        return goodsMapper.findAll();
    }

    @Override
    public List<Goods> search(String keyword) {
        return goodsMapper.search(keyword);
    }

    @Override
    public List<Goods> listBrands() {
        return goodsMapper.listBrands();
    }

    @Override
    public List<Goods> searchWithFilter(String keyword, String brand, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice) {
        return goodsMapper.searchWithFilter(keyword, brand, minPrice, maxPrice);
    }

    @Override
    public Goods findById(java.lang.Long id) {
        return goodsMapper.findById(id);
    }

    @Override
    public void updateStatus(java.lang.Long id, Integer status) {
        goodsMapper.updateStatus(id, status);
    }

    @Override
    public List<Goods> listMyGoods(java.lang.Long userId) {
        return goodsMapper.listByUserId(userId);
    }

    @Override
    public List<Goods> listPending() {
        return goodsMapper.listPending();
    }

    @Override
    public void approve(java.lang.Long id) {
        goodsMapper.updateStatus(id, 1);
    }

    @Override
    public void reject(java.lang.Long id) {
        goodsMapper.updateStatus(id, 2);
    }

    @Override
    public void delete(java.lang.Long id) {
        goodsMapper.delete(id);
    }

    @Override
    public boolean decreaseStock(Long id, Integer quantity) {
        int result = goodsMapper.decreaseStock(id, quantity);
        return result > 0;
    }

    @Override
    public void increaseStock(Long id, Integer quantity) {
        goodsMapper.increaseStock(id, quantity);
    }

    @Override
    public void update(Goods goods) {
        if (goods.getId() == null) {
            throw new RuntimeException("商品ID不能为空");
        }
        Goods existing = goodsMapper.findById(goods.getId());
        if (existing == null) {
            throw new RuntimeException("商品不存在");
        }
        if (goods.getSellerId() != null && !goods.getSellerId().equals(existing.getSellerId())) {
            throw new RuntimeException("无权编辑该商品");
        }
        if (goods.getStatus() == null) {
            goods.setStatus(existing.getStatus());
        }
        int updated = goodsMapper.update(goods);
        if (updated <= 0) {
            throw new RuntimeException("商品更新失败");
        }
    }

}

