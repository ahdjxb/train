package com.train.ticket.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * T→E 第二段行程DTO
 */
@Data
public class TtoETransferDTO {
    private Long transferStationId;    // 中转站T
    private Long trainId2;            // 第二段车次id
    private LocalDateTime departTimeTransfer; // 第二段从中转站T发车时间
    private String trainNo2;
    private String transferStationName;
    private String endStationName;
    private LocalDateTime arriveTime2;
}
