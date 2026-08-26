package com.train.ticket.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SeatVO {
    private Long seatId;
    private Long carriageId;
    private String seatNo;
    private String carriageNo;
    private String carriageLevel;
    private BigDecimal price;
}
