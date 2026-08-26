package com.train.ticket.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * S→T 第一段行程DTO
 */
@Data
public class StoTTransferDTO {
    private Long transferStationId;   // 中转站T
    private Long trainId1;            // 第一段车次id
    private LocalDateTime arriveTimeTransfer; // 第一段到达中转站T的时间
    private String trainNo1;
    private String startStationName;
    private String transferStationName;
    private LocalDateTime departTime1;
}
