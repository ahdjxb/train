package com.train.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("change_record")
public class ChangeRecord {

    @TableId("change_id")
    private Long changeId;

    private Long oldOrderId;

    private Long newOrderId;

    private LocalDateTime changeTime;

    private BigDecimal priceDiff;
}
