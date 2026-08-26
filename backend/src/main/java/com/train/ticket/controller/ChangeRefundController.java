package com.train.ticket.controller;

import com.train.ticket.common.Result;
import com.train.ticket.dto.ChangeOrderDTO;
import com.train.ticket.dto.OrderDetailVO;
import com.train.ticket.dto.OrderQueryDTO;
import com.train.ticket.dto.RefundResultVO;
import com.train.ticket.service.ChangeRefundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 改签退票Controller
 */
@RestController
@RequestMapping("/api/order")
public class ChangeRefundController {

    @Autowired
    private ChangeRefundService changeRefundService;

    /**
     * 个人订单多条件筛选查询
     */
    @PostMapping("/query")
    public Result<List<OrderDetailVO>> queryOrders(@RequestBody OrderQueryDTO dto) {
        return Result.success(changeRefundService.queryOrders(dto));
    }

    /**
     * 改签
     */
    @PostMapping("/change/{orderId}")
    public Result<OrderDetailVO> changeOrder(@PathVariable Long orderId, @RequestBody ChangeOrderDTO dto) {
        dto.setOldOrderId(orderId);
        return Result.success(changeRefundService.changeOrder(dto));
    }

    /**
     * 退票
     */
    @PostMapping("/refund/{orderId}")
    public Result<RefundResultVO> refundOrder(@PathVariable Long orderId) {
        return Result.success(changeRefundService.refundOrder(orderId));
    }

    /**
     * 用户主动取消订单
     */
    @PutMapping("/cancel/{orderId}")
    public Result<?> cancelOrder(@PathVariable Long orderId) {
        changeRefundService.cancelOrder(orderId);
        return Result.success();
    }
}
