package com.train.ticket.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 退票结果VO
 */
@Data
public class RefundResultVO {
    private Long orderId;
    private BigDecimal refundFee;
    private BigDecimal refundAmount;
}
