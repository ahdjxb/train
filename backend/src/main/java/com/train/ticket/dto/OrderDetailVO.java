package com.train.ticket.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单详情VO（扩展版，含车次/车厢/座位/站点完整信息）
 */
@Data
public class OrderDetailVO {
    private Long orderId;
    private Long userId;
    private String username;
    private Long passengerId;
    private String passengerName;
    private String passengerIdCard;
    private Long trainId;
    private String trainNo;
    private String trainType;
    private String departTime;
    private String arriveTime;
    private Long startStationId;
    private String startStationName;
    private String startCity;
    private Long endStationId;
    private String endStationName;
    private String endCity;
    private Long seatId;
    private String seatNo;
    private Long carriageId;
    private String carriageNo;
    private String carriageLevel;
    private java.math.BigDecimal ticketPrice;
    private String orderStatus;
    private LocalDateTime createTime;
    private LocalDateTime payDeadline;
}
