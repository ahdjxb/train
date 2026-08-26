package com.train.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("refund_record")
public class RefundRecord {

    @TableId("refund_id")
    private Long refundId;

    private Long orderId;

    private LocalDateTime refundTime;

    private BigDecimal refundFee;
}
