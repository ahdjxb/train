package com.train.ticket.controller;

import com.train.ticket.common.Result;
import com.train.ticket.dto.LoginDTO;
import com.train.ticket.dto.RegisterDTO;
import com.train.ticket.dto.UpdateUserDTO;
import com.train.ticket.dto.ChangePasswordDTO;
import com.train.ticket.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @GetMapping("/info/{userId}")
    public Result<?> getUserInfo(@PathVariable Long userId) {
        return Result.success(userService.getUserInfo(userId));
    }

    @PutMapping("/info/{userId}")
    public Result<?> updateUserInfo(@PathVariable Long userId, @RequestBody UpdateUserDTO dto) {
        userService.updateUserInfo(userId, dto);
        return Result.success();
    }

    @PutMapping("/password/{userId}")
    public Result<?> changePassword(@PathVariable Long userId, @Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(userId, dto);
        return Result.success();
    }

    @DeleteMapping("/{userId}")
    public Result<?> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return Result.success();
    }
}
