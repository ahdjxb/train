package com.train.ticket.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 改签DTO：指定新车次、新座位
 */
@Data
public class ChangeOrderDTO {
    /** 原始订单ID */
    private Long oldOrderId;
    /** 新车次ID */
    private Long newTrainId;
    /** 新座位ID */
    private Long newSeatId;
}
