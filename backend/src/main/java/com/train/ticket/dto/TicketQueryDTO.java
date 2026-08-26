package com.train.ticket.dto;

import lombok.Data;

@Data
public class TicketQueryDTO {
    /** 始发城市 */
    private String startCity;
    /** 终点城市 */
    private String endCity;
    /** 始发站点ID（可选，筛选特定站点） */
    private Long startStationId;
    /** 终点站点ID（可选，筛选特定站点） */
    private Long endStationId;
    /** 查询日期 yyyy-MM-dd */
    private String date;
    /** 筛选类型：direct直达 / transfer中转 / 空字符串全部 */
    private String filterType;
    /** 车型筛选：高铁/普通/空字符串全部 */
    private String trainType;
}
