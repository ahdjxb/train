package com.train.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("train_station_route")
public class TrainStationRoute {

    @TableId("route_id")
    private Long routeId;

    private Long trainId;

    private Long stationId;

    /** 途经顺序 */
    private Integer sort;

    /** 到达该站时间 */
    private LocalDateTime arriveDatetime;

    /** 从该站发车时间 */
    private LocalDateTime departDatetime;
}
