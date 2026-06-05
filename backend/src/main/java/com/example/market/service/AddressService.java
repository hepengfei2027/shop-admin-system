package com.example.market.service;

import com.example.market.entity.Address;

import java.util.List;

public interface AddressService {

    List<Address> listByUserId(Long userId);

    Address addAddress(Long userId, Address address);

    Address updateAddress(Long userId, Address address);

    void deleteAddress(Long userId, Long id);
}