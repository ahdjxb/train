package com.train.ticket.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TrainDTO {

    private Long trainId;
    private String trainNo;
    private LocalDateTime departDatetime;
    private LocalDateTime arriveDatetime;
    private Long startStationId;
    private Long endStationId;
    private String trainType;
    private Integer saleStatus;

    /** 途经站点列表 */
    private List<RouteItem> routeList;

    @Data
    public static class RouteItem {
        private Long stationId;
        private Integer sort;
        private LocalDateTime arriveDatetime;
        private LocalDateTime departDatetime;
    }
}
