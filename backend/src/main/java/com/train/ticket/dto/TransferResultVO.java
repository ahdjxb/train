package com.train.ticket.dto;

import lombok.Data;
import java.util.List;

@Data
public class TransferResultVO {
    private Integer transferCount;
    private List<String> stationNames;
    private String totalDuration;
    private Long totalMinutes;
    private Long transferStationId;
    private String transferStationName;
    private Boolean hasTicket;
    private List<TransferSegment> segments;

    @Data
    public static class TransferSegment {
        private Long trainId;
        private String trainNo;
        private String trainType;
        private String startStationName;
        private String endStationName;
        private String departTime;
        private String arriveTime;
        private Boolean hasTicket;
    }
}
