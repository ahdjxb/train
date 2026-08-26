package com.train.ticket.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderVO {
    private Long orderId;
    private Long userId;
    private Long passengerId;
    private String passengerName;
    private Long trainId;
    private String trainNo;
    private Long seatId;
    private String seatNo;
    private String carriageLevel;
    private BigDecimal ticketPrice;
    private String orderStatus;
    private LocalDateTime createTime;
    /** 支付截止时间（创建时间+30分钟） */
    private LocalDateTime payDeadline;
}
