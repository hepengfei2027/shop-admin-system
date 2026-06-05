package com.example.market.service.impl;

import com.example.market.entity.Address;
import com.example.market.mapper.AddressMapper;
import com.example.market.service.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    @Resource
    private AddressMapper addressMapper;

    @Override
    public List<Address> listByUserId(Long userId) {
        return addressMapper.listByUserId(userId);
    }

    @Override
    @Transactional
    public Address addAddress(Long userId, Address address) {
        address.setUserId(userId);
        if (address.getIsDefault() == null) {
            address.setIsDefault(false);
        }
        if (address.getIsDefault()) {
            addressMapper.clearDefault(userId);
        }
        addressMapper.insert(address);
        return address;
    }

    @Override
    @Transactional
    public Address updateAddress(Long userId, Address address) {
        Address existing = addressMapper.listByUserId(userId).stream()
                .filter(a -> a.getId().equals(address.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("地址不存在"));
        
        if (address.getIsDefault() != null && address.getIsDefault() && !existing.getIsDefault()) {
            addressMapper.clearDefault(userId);
        }
        addressMapper.update(address);
        return address;
    }

    @Override
    public void deleteAddress(Long userId, Long id) {
        Address existing = addressMapper.listByUserId(userId).stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("地址不存在"));
        addressMapper.delete(id);
    }
}