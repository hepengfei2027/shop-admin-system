package com.example.market.controller;

import com.example.market.dto.Result;
import com.example.market.entity.Address;
import com.example.market.service.AddressService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/address")
@CrossOrigin
public class AddressController {

    @Resource
    private AddressService addressService;

    @GetMapping("/list")
    public Result<List<Address>> list(@RequestParam Long userId) {
        return Result.ok(addressService.listByUserId(userId));
    }

    @PostMapping("/add")
    public Result<Address> add(@RequestBody Address address) {
        return Result.ok(addressService.addAddress(address.getUserId(), address));
    }

    @PostMapping("/update")
    public Result<Address> update(@RequestBody Address address) {
        return Result.ok(addressService.updateAddress(address.getUserId(), address));
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id, @RequestParam Long userId) {
        addressService.deleteAddress(userId, id);
        return Result.ok(null);
    }
}