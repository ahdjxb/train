package com.train.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("seat")
public class Seat {

    @TableId("seat_id")
    private Long seatId;

    private Long carriageId;

    /** 真实座位号 */
    private String seatNo;
}
