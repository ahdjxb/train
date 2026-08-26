package com.train.ticket.controller;

import com.train.ticket.common.Result;
import com.train.ticket.dto.OrderCreateDTO;
import com.train.ticket.dto.OrderVO;
import com.train.ticket.dto.SeatVO;
import com.train.ticket.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 购票下单
     */
    @PostMapping
    public Result<OrderVO> createOrder(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.success(orderService.createOrder(dto));
    }

    /**
     * 支付订单
     */
    @PutMapping("/pay/{orderId}")
    public Result<?> payOrder(@PathVariable Long orderId) {
        orderService.payOrder(orderId);
        return Result.success();
    }

    /**
     * 查询订单详情
     */
    @GetMapping("/{orderId}")
    public Result<OrderVO> getOrder(@PathVariable Long orderId) {
        return Result.success(orderService.getOrder(orderId));
    }

    /**
     * 查询用户订单列表
     */
    @GetMapping("/list/{userId}")
    public Result<List<OrderVO>> listUserOrders(
            @PathVariable Long userId,
            @RequestParam(required = false) String orderStatus) {
        return Result.success(orderService.listUserOrders(userId, orderStatus));
    }

    /**
     * 查询车次可用座位列表（供选座）
     */
    @GetMapping("/available-seats/{trainId}")
    public Result<List<SeatVO>> listAvailableSeats(@PathVariable Long trainId) {
        return Result.success(orderService.listAvailableSeats(trainId));
    }

    /**
     * 超时取消订单（前端倒计时到期后调用）
     */
    @PutMapping("/timeout-cancel/{orderId}")
    public Result<?> timeoutCancelOrder(@PathVariable Long orderId) {
        orderService.timeoutCancelOrder(orderId);
        return Result.success();
    }
}
