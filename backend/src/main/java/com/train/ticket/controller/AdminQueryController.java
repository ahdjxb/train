package com.train.ticket.controller;

import com.train.ticket.common.Result;
import com.train.ticket.dto.OrderDetailVO;
import com.train.ticket.dto.OrderQueryDTO;
import com.train.ticket.dto.TicketStatsVO;
import com.train.ticket.entity.TUser;
import com.train.ticket.service.AdminQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员查询Controller：全部车票、全部订单、统计、用户管理
 */
@RestController
@RequestMapping("/api/admin/query")
public class AdminQueryController {

    @Autowired
    private AdminQueryService adminQueryService;

    /**
     * 查询全部订单（仅查看）
     */
    @PostMapping("/orders")
    public Result<List<OrderDetailVO>> queryAllOrders(@RequestBody OrderQueryDTO dto) {
        return Result.success(adminQueryService.queryAllOrders(dto));
    }

    /**
     * 查询全部车票（已支付/已改签/已完成）
     */
    @PostMapping("/tickets")
    public Result<List<OrderDetailVO>> queryAllTickets(@RequestBody OrderQueryDTO dto) {
        return Result.success(adminQueryService.queryAllTickets(dto));
    }

    /**
     * 车票统计：余量、出票量、退票量、异常检测
     */
    @GetMapping("/stats")
    public Result<TicketStatsVO> getTicketStats() {
        return Result.success(adminQueryService.getTicketStats());
    }

    /**
     * 查询全部普通用户
     */
    @GetMapping("/users")
    public Result<List<TUser>> listNormalUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer isLock) {
        return Result.success(adminQueryService.listNormalUsers(username, isLock));
    }

    /**
     * 锁定/解锁用户
     */
    @PutMapping("/users/{userId}/lock")
    public Result<?> setUserLockStatus(
            @PathVariable Long userId,
            @RequestParam Integer isLock) {
        adminQueryService.setUserLockStatus(userId, isLock);
        return Result.success();
    }
}
