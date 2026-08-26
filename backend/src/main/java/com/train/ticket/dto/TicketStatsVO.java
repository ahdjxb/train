package com.train.ticket.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TicketStatsVO {
    private Integer totalTrains;
    private Integer totalSeats;
    private Integer soldCount;
    private Integer pendingCount;
    private Integer availableCount;
    private Integer refundCount;
    private Integer issuedCount;
    private Map<String, Integer> statsByType;
    private List<DuplicateSeatVO> duplicateSeats;
    private List<TrainTicketStatsVO> trainStatsList;

    @Data
    public static class DuplicateSeatVO {
        private Long seatId;
        private String seatNo;
        private Long trainId;
        private String trainNo;
        private Integer activeOrderCount;
    }

    @Data
    public static class TrainTicketStatsVO {
        private Long trainId;
        private String trainNo;
        private String trainType;
        private String departDatetime;
        private String startStationName;
        private String endStationName;
        private Integer totalSeats;
        private Integer availableCount;
        private Integer issuedCount;
        private Integer pendingCount;
        private Integer refundCount;
    }
}
