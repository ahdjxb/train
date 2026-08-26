package com.train.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("passenger")
public class Passenger {

    @TableId("passenger_id")
    private Long passengerId;

    private Long userId;

    private String realName;

    private String idCard;
}
