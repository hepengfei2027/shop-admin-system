package com.example.market.service;

import com.example.market.entity.User;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;

public interface UserService {

    User register(String username, String password, String nickname, String phone, Integer role);

    User login(String username, String password, HttpSession session);

    User findById(Long id);

    void updateStatus(Long id, Integer status, LocalDateTime bannedUntil);

    void updateUserInfo(Long id, String nickname, Integer role, String phone);

    void updateAvatar(Long id, String avatar);

    List<User> findAll();
}

