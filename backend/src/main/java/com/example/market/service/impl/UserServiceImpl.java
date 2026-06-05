package com.example.market.service.impl;

import com.example.market.entity.User;
import com.example.market.mapper.UserMapper;
import com.example.market.service.UserService;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Override
    public User register(String username, String password, String nickname, String phone, Integer role) {
        User exist = userMapper.findByUsername(username);
        if (exist != null) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        user.setNickname(nickname);
        user.setPhone(phone);
        if (role != null && (role == 0 || role == 2)) {
            user.setRole(role);
        } else {
            user.setRole(0);
        }
        userMapper.insert(user);
        return user;
    }

    @Override
    public User login(String username, String password, HttpSession session) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 检查用户状态
        if (user.getStatus() == 1) {
            if (user.getBannedUntil() != null && user.getBannedUntil().isAfter(LocalDateTime.now())) {
                long hours = java.time.Duration.between(LocalDateTime.now(), user.getBannedUntil()).toHours();
                throw new RuntimeException("账号已被封禁，剩余封禁时间：" + hours + "小时");
            } else {
                // 封禁时间已过，自动解除封禁
                user.setStatus(0);
                user.setBannedUntil(null);
                userMapper.updateStatus(user.getId(), 0, null);
            }
        }
        String md5 = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!md5.equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        session.setAttribute("user", user);
        return user;
    }

    @Override
    public User findById(java.lang.Long id) {
        return userMapper.findById(id);
    }

    @Override
    public void updateStatus(java.lang.Long id, Integer status, LocalDateTime bannedUntil) {
        userMapper.updateStatus(id, status, bannedUntil);
    }

    @Override
    public List<User> findAll() {
        return userMapper.findAll();
    }

    @Override
    public void updateUserInfo(java.lang.Long id, String nickname, Integer role, String phone) {
        userMapper.updateUserInfo(id, nickname, role, phone);
    }

    @Override
    public void updateAvatar(java.lang.Long id, String avatar) {
        userMapper.updateAvatar(id, avatar);
    }
}

