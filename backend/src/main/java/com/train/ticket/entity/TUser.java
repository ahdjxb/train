package com.train.ticket.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_user")
public class TUser {

    @TableId("user_id")
    private Long userId;

    private String username;

    private String account;

    private String password;

    private String phone;

    /** 角色：NORMAL/ADMIN */
    private String role;

    /** 是否锁定：0=未锁定，1=锁定 */
    private Integer isLock;
}
