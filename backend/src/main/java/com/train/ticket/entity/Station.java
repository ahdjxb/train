package com.train.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("station")
public class Station {

    @TableId("station_id")
    private Long stationId;

    private String stationName;

    private String city;
}
