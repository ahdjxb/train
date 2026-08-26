package com.train.ticket.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderCreateDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "乘车人ID不能为空")
    private Long passengerId;

    @NotNull(message = "车次ID不能为空")
    private Long trainId;

    @NotNull(message = "座位ID不能为空")
    private Long seatId;
}
