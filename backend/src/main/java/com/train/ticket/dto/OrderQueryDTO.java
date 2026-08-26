package com.train.ticket.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单多条件筛选查询DTO（用户端+管理员端通用）
 */
@Data
public class OrderQueryDTO {

    // ===== 用户端专用 =====
    private Long userId;

    // ===== 通用筛选条件 =====
    /** 时间范围-起 */
    private String createTimeStart;
    /** 时间范围-止 */
    private String createTimeEnd;
    /** 订单状态 */
    private String orderStatus;
    /** 车次号 */
    private String trainNo;
    /** 出发站ID */
    private Long startStationId;
    /** 到达站ID */
    private Long endStationId;
    /** 乘车日期 yyyy-MM-dd */
    private String travelDate;
    /** 车厢号 */
    private String carriageNo;
    /** 座位号 */
    private String seatNo;
    /** 订单编号 */
    private Long orderId;
}
