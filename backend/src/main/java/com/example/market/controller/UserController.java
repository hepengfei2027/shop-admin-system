package com.example.market.controller;

import com.example.market.dto.Result;
import com.example.market.entity.User;
import com.example.market.service.UserService;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    public Result<User> register(@RequestBody User user) {
        User u = userService.register(
                user.getUsername(),
                user.getPassword(),
                user.getNickname(),
                user.getPhone(),
                user.getRole()
        );
        return Result.ok(u);
    }

    @PostMapping("/login")
    public Result<User> login(@RequestBody User user, HttpSession session) {
        try {
            User u = userService.login(user.getUsername(), user.getPassword(), session);
            return Result.ok(u);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<User> detail(@PathVariable Long id) {
        return Result.ok(userService.findById(id));
    }

    @GetMapping("/list")
    public Result<List<User>> list() {
        return Result.ok(userService.findAll());
    }

    @PostMapping("/updateStatus/{id}")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status, @RequestParam(required = false) Long banHours) {
        LocalDateTime bannedUntil = null;
        if (status == 1 && banHours != null) {
            bannedUntil = LocalDateTime.now().plusHours(banHours);
        }
        userService.updateStatus(id, status, bannedUntil);
        return Result.ok(null);
    }

    @PostMapping("/update/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestParam String nickname, @RequestParam Integer role, @RequestParam(required = false) String phone) {
        userService.updateUserInfo(id, nickname, role, phone);
        return Result.ok(null);
    }

    @PostMapping("/{id}/avatar")
    public Result<Void> updateAvatar(@PathVariable Long id, @RequestParam String avatar) {
        userService.updateAvatar(id, avatar);
        return Result.ok(null);
    }
}

