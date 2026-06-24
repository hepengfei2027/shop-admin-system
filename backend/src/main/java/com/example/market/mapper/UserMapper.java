package com.example.market.mapper;

import com.example.market.entity.User;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Long id);

    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT * FROM user")
    List<User> findAll();

    @Insert("INSERT INTO user(username, password, nickname, phone, role, status, banned_until, create_time, member_level, experience, discount, balance) " +
            "VALUES(#{username}, #{password}, #{nickname}, #{phone}, #{role}, #{status}, #{bannedUntil}, NOW(), 1, 0, 1.0, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE user SET nickname = #{nickname}, phone = #{phone} WHERE id = #{id}")
    int update(User user);

    @Update("UPDATE user SET status = #{status}, banned_until = #{bannedUntil} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("bannedUntil") LocalDateTime bannedUntil);

    @Update("UPDATE user SET nickname = #{nickname}, role = #{role}, phone = #{phone} WHERE id = #{id}")
    int updateUserInfo(@Param("id") Long id, @Param("nickname") String nickname, @Param("role") Integer role, @Param("phone") String phone);

    @Update("UPDATE user SET avatar = #{avatar} WHERE id = #{id}")
    int updateAvatar(@Param("id") Long id, @Param("avatar") String avatar);

    @Delete("DELETE FROM user WHERE id = #{id}")
    int delete(Long id);
    
    @Select("SELECT COUNT(*) FROM user")
    long count();
    
    @Update("UPDATE user SET member_level = #{memberLevel}, experience = #{experience}, discount = #{discount} WHERE id = #{id}")
    int updateMemberInfo(@Param("id") Long id, @Param("memberLevel") Integer memberLevel, 
                        @Param("experience") Integer experience, @Param("discount") Double discount);

    @Update("UPDATE user SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    // 余额相关
    @Update("UPDATE user SET balance = balance + #{amount} WHERE id = #{id}")
    int addBalance(@Param("id") Long id, @Param("amount") java.math.BigDecimal amount);

    @Update("UPDATE user SET balance = balance - #{amount} WHERE id = #{id} AND balance >= #{amount}")
    int deductBalance(@Param("id") Long id, @Param("amount") java.math.BigDecimal amount);
}

