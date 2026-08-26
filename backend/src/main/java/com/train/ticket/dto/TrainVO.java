package com.train.ticket.dto;

import lombok.Data;

@Data
public class TrainVO {
    private Long trainId;
    private String trainNo;
    private String departDatetime;
    private String arriveDatetime;
    private Long startStationId;
    private String startStationName;
    private Long endStationId;
    private String endStationName;
    private String trainType;
    private Integer saleStatus;
    /** 售票开放时间：发车前15天16:00 */
    private String saleOpenTime;
    /** 是否已发车 */
    private Boolean departed;
    /** 是否在默认售票窗口期内 */
    private Boolean inSaleWindow;
    private java.util.List<TrainDTO.RouteItem> routeList;
}
