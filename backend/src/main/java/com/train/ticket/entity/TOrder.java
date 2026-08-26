package com.train.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class TOrder {

    @TableId("order_id")
    private Long orderId;

    private Long userId;

    private Long passengerId;

    private Long trainId;

    private Long seatId;

    private BigDecimal ticketPrice;

    /** 订单状态：PENDING/PAID/CHANGED/REFUNDED/FINISHED/CANCELLED */
    private String orderStatus;

    private LocalDateTime createTime;
}
