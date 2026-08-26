package com.train.ticket.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 余票查询结果项（直达车次）
 */
@Data
public class TicketResultVO {

    private Long trainId;
    private String trainNo;
    private String trainType;
    private String departTime;
    private String arriveTime;
    private Long startStationId;
    private String startStationName;
    private Long endStationId;
    private String endStationName;
    private Integer saleStatus;
    private String departDatetime;
    private String arriveDatetime;

    /** 运行时长（如"02:30"） */
    private String duration;

    /** 席位余票列表 */
    private List<SeatAvailabilityVO> seatList;

    /** 是否有余票 */
    private Boolean hasTicket;

    @Data
    public static class SeatAvailabilityVO {
        private String carriageLevel;
        private Integer totalCount;
        private Integer availableCount;
        /** 席位票价 */
        private java.math.BigDecimal price;
    }
}
