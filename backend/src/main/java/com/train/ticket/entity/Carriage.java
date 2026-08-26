package com.train.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("carriage")
public class Carriage {

    @TableId("carriage_id")
    private Long carriageId;

    private Long trainId;

    /** 车厢号 */
    private String carriageNo;

    /** 席位等级：商务/一等/二等 */
    private String carriageLevel;
}
