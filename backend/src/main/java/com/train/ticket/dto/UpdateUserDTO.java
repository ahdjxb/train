package com.train.ticket.dto;

import lombok.Data;

@Data
public class UpdateUserDTO {
    private String username;
    private String account;
    private String password;
    private String phone;
}
