package com.train.ticket.common;

public class Constants {

    // 角色
    public static final String ROLE_NORMAL = "NORMAL";
    public static final String ROLE_ADMIN = "ADMIN";

    // 订单状态
    public static final String ORDER_PENDING = "PENDING";       // 待支付
    public static final String ORDER_PAID = "PAID";             // 已支付
    public static final String ORDER_CHANGED = "CHANGED";       // 已改签
    public static final String ORDER_REFUNDING = "REFUNDING";   // 退票中
    public static final String ORDER_REFUNDED = "REFUNDED";     // 已退票
    public static final String ORDER_FINISHED = "FINISHED";    // 已完成
    public static final String ORDER_CANCELLED = "CANCELLED";   // 已取消(超时未付)

    // 售票状态 0=未开售 1=已开售
    public static final int SALE_STATUS_CLOSED = 0;
    public static final int SALE_STATUS_OPEN = 1;
}
