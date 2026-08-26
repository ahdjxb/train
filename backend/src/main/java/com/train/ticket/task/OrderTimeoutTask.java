package com.train.ticket.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.train.ticket.common.Constants;
import com.train.ticket.entity.TOrder;
import com.train.ticket.mapper.TOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务：作为兜底保障，每5分钟扫描一次超时未支付订单。
 * 主要取消逻辑由前端倒计时触发，此处仅做兜底防止遗漏。
 */
@Component
public class OrderTimeoutTask {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutTask.class);

    @Autowired
    private TOrderMapper orderMapper;

    @Scheduled(fixedRate = 300000) // 每5分钟兜底检查一次
    public void cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(30);

        List<TOrder> timeoutOrders = orderMapper.selectList(
                new LambdaQueryWrapper<TOrder>()
                        .eq(TOrder::getOrderStatus, Constants.ORDER_PENDING)
                        .lt(TOrder::getCreateTime, deadline));

        if (timeoutOrders.isEmpty()) {
            return;
        }

        for (TOrder order : timeoutOrders) {
            order.setOrderStatus(Constants.ORDER_CANCELLED);
            orderMapper.updateById(order);
            log.info("订单超时取消(兜底)：orderId={}, createTime={}", order.getOrderId(), order.getCreateTime());
        }

        log.info("本次共取消超时订单 {} 笔", timeoutOrders.size());
    }
}
