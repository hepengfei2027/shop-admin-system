package com.example.market.mapper;

import com.example.market.entity.Address;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AddressMapper {

    @Select("SELECT * FROM addresses WHERE user_id = #{userId} ORDER BY is_default DESC, create_time DESC")
    List<Address> listByUserId(Long userId);

    @Insert("INSERT INTO addresses(user_id, name, phone, province, city, district, detail, is_default, create_time) VALUES(#{userId}, #{name}, #{phone}, #{province}, #{city}, #{district}, #{detail}, #{isDefault}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Address address);

    @Update("UPDATE addresses SET name = #{name}, phone = #{phone}, province = #{province}, city = #{city}, district = #{district}, detail = #{detail}, is_default = #{isDefault} WHERE id = #{id}")
    int update(Address address);

    @Delete("DELETE FROM addresses WHERE id = #{id}")
    int delete(Long id);

    @Update("UPDATE addresses SET is_default = false WHERE user_id = #{userId} AND is_default = true")
    int clearDefault(Long userId);
}