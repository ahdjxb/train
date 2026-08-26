package com.train.ticket.controller;

import com.train.ticket.common.Result;
import com.train.ticket.dto.ChangePasswordDTO;
import com.train.ticket.dto.LoginDTO;
import com.train.ticket.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(adminService.login(dto));
    }

    @PutMapping("/password/{userId}")
    public Result<?> changePassword(@PathVariable Long userId, @Valid @RequestBody ChangePasswordDTO dto) {
        adminService.changePassword(userId, dto);
        return Result.success();
    }
}
