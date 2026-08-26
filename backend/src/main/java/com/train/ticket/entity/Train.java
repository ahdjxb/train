package com.train.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("train")
public class Train {

    @TableId("train_id")
    private Long trainId;

    /** 车次号，如 G123 */
    private String trainNo;

    /** 发车时间 */
    private LocalDateTime departDatetime;

    /** 到达时间 */
    private LocalDateTime arriveDatetime;

    /** 始发站ID */
    private Long startStationId;

    /** 终点站ID */
    private Long endStationId;

    /** 车型：高铁/普通 */
    private String trainType;

    /** 售票状态：0=未开售，1=已开售 */
    private Integer saleStatus;
}
