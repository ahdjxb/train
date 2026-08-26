package com.train.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.train.ticket.common.Constants;
import com.train.ticket.dto.OrderCreateDTO;
import com.train.ticket.dto.OrderVO;
import com.train.ticket.dto.SeatVO;
import com.train.ticket.entity.*;
import com.train.ticket.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private TOrderMapper orderMapper;
    @Autowired
    private TUserMapper userMapper;
    @Autowired
    private PassengerMapper passengerMapper;
    @Autowired
    private TrainMapper trainMapper;
    @Autowired
    private SeatMapper seatMapper;
    @Autowired
    private CarriageMapper carriageMapper;
    @Autowired
    private TicketQueryService ticketQueryService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 购票下单
     * 1. 校验售票开放时间：发车前15天16:00
     * 2. 校验座位未被占用（PENDING/PAID）
     * 3. 生成待支付订单，锁定座位
     */
    @Transactional
    public OrderVO createOrder(OrderCreateDTO dto) {
        // 校验用户
        TUser user = userMapper.selectById(dto.getUserId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getIsLock() != null && user.getIsLock() == 1) {
            throw new RuntimeException("账号已被锁定");
        }

        // 校验乘车人
        Passenger passenger = passengerMapper.selectById(dto.getPassengerId());
        if (passenger == null || !passenger.getUserId().equals(dto.getUserId())) {
            throw new RuntimeException("乘车人不存在或不属于当前用户");
        }

        // 校验车次
        Train train = trainMapper.selectById(dto.getTrainId());
        if (train == null) {
            throw new RuntimeException("车次不存在");
        }

        // 校验售票开放时间：发车前15天16:00
        LocalDateTime saleOpenTime = train.getDepartDatetime()
                .minusDays(15)
                .withHour(16).withMinute(0).withSecond(0).withNano(0);
        if (LocalDateTime.now().isBefore(saleOpenTime)) {
            throw new RuntimeException("未到售票开放时间，售票开放时间为：" + saleOpenTime.format(FORMATTER));
        }

        // 校验车次是否已发车
        if (train.getDepartDatetime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("车次已发车，无法购票");
        }

        // 校验售票状态
        if (train.getSaleStatus() == null || train.getSaleStatus() == Constants.SALE_STATUS_CLOSED) {
            throw new RuntimeException("该车次尚未开放售票");
        }

        // 校验座位
        Seat seat = seatMapper.selectById(dto.getSeatId());
        if (seat == null) {
            throw new RuntimeException("座位不存在");
        }
        Carriage carriage = carriageMapper.selectById(seat.getCarriageId());
        if (carriage == null || !carriage.getTrainId().equals(dto.getTrainId())) {
            throw new RuntimeException("座位不属于该车次");
        }

        // 校验座位是否被占用（防止超卖）
        Long occupied = orderMapper.selectCount(
                new LambdaQueryWrapper<TOrder>()
                        .eq(TOrder::getTrainId, dto.getTrainId())
                        .eq(TOrder::getSeatId, dto.getSeatId())
                        .in(TOrder::getOrderStatus,
                                Constants.ORDER_PENDING, Constants.ORDER_PAID));
        if (occupied > 0) {
            throw new RuntimeException("该座位已被占用");
        }

        // 创建待支付订单
        TOrder order = new TOrder();
        order.setUserId(dto.getUserId());
        order.setPassengerId(dto.getPassengerId());
        order.setTrainId(dto.getTrainId());
        order.setSeatId(dto.getSeatId());
        // 简单定价：根据席位等级设定
        order.setTicketPrice(getPriceByLevel(carriage.getCarriageLevel()));
        order.setOrderStatus(Constants.ORDER_PENDING);
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insert(order);

        return convertToOrderVO(order, passenger, train, seat, carriage);
    }

    /**
     * 支付订单：待支付 → 已支付
     */
    @Transactional
    public void payOrder(Long orderId) {
        TOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!Constants.ORDER_PENDING.equals(order.getOrderStatus())) {
            throw new RuntimeException("订单状态不支持支付");
        }
        // 校验是否超时（30分钟）
        LocalDateTime deadline = order.getCreateTime().plusMinutes(30);
        if (LocalDateTime.now().isAfter(deadline)) {
            // 超时自动取消
            order.setOrderStatus(Constants.ORDER_CANCELLED);
            orderMapper.updateById(order);
            throw new RuntimeException("支付超时，订单已取消");
        }

        order.setOrderStatus(Constants.ORDER_PAID);
        orderMapper.updateById(order);
    }

    /**
     * 查询订单详情
     */
    public OrderVO getOrder(Long orderId) {
        TOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        Passenger passenger = passengerMapper.selectById(order.getPassengerId());
        Train train = trainMapper.selectById(order.getTrainId());
        Seat seat = seatMapper.selectById(order.getSeatId());
        Carriage carriage = seat != null ? carriageMapper.selectById(seat.getCarriageId()) : null;
        return convertToOrderVO(order, passenger, train, seat, carriage);
    }

    /**
     * 查询用户订单列表
     */
    public List<OrderVO> listUserOrders(Long userId, String orderStatus) {
        LambdaQueryWrapper<TOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TOrder::getUserId, userId);
        if (orderStatus != null && !orderStatus.isEmpty()) {
            wrapper.eq(TOrder::getOrderStatus, orderStatus);
        }
        wrapper.orderByDesc(TOrder::getCreateTime);
        List<TOrder> orders = orderMapper.selectList(wrapper);

        return orders.stream().map(order -> {
            Passenger passenger = passengerMapper.selectById(order.getPassengerId());
            Train train = trainMapper.selectById(order.getTrainId());
            Seat seat = seatMapper.selectById(order.getSeatId());
            Carriage carriage = seat != null ? carriageMapper.selectById(seat.getCarriageId()) : null;
            return convertToOrderVO(order, passenger, train, seat, carriage);
        }).toList();
    }

    /**
     * 查询车次可用座位列表（供选座使用），返回带车厢信息的SeatVO
     */
    public List<SeatVO> listAvailableSeats(Long trainId) {
        Train train = trainMapper.selectById(trainId);
        if (train == null) {
            throw new RuntimeException("车次不存在");
        }
        List<Carriage> carriages = carriageMapper.selectList(
                new LambdaQueryWrapper<Carriage>().eq(Carriage::getTrainId, trainId));
        if (carriages.isEmpty()) {
            return List.of();
        }
        List<SeatVO> result = new ArrayList<>();
        for (Carriage carriage : carriages) {
            List<Seat> seats = seatMapper.selectList(
                    new LambdaQueryWrapper<Seat>().eq(Seat::getCarriageId, carriage.getCarriageId()));
            for (Seat seat : seats) {
                Long occupied = orderMapper.selectCount(
                        new LambdaQueryWrapper<TOrder>()
                                .eq(TOrder::getTrainId, trainId)
                                .eq(TOrder::getSeatId, seat.getSeatId())
                                .in(TOrder::getOrderStatus,
                                        Constants.ORDER_PENDING, Constants.ORDER_PAID));
                if (occupied == 0) {
                    SeatVO vo = new SeatVO();
                    vo.setSeatId(seat.getSeatId());
                    vo.setCarriageId(carriage.getCarriageId());
                    vo.setSeatNo(seat.getSeatNo());
                    vo.setCarriageNo(carriage.getCarriageNo());
                    vo.setCarriageLevel(carriage.getCarriageLevel());
                    vo.setPrice(getPriceByLevel(carriage.getCarriageLevel()));
                    result.add(vo);
                }
            }
        }
        return result;
    }

    /**
     * 超时取消订单（前端倒计时到期后调用）
     */
    @Transactional
    public void timeoutCancelOrder(Long orderId) {
        TOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        if (Constants.ORDER_PENDING.equals(order.getOrderStatus())) {
            order.setOrderStatus(Constants.ORDER_CANCELLED);
            orderMapper.updateById(order);
        }
    }

    // ==================== 私有方法 ====================

    private BigDecimal getPriceByLevel(String level) {
        if ("商务".equals(level)) return new BigDecimal("500.00");
        if ("一等".equals(level)) return new BigDecimal("300.00");
        if ("二等".equals(level)) return new BigDecimal("150.00");
        return new BigDecimal("100.00");
    }

    private OrderVO convertToOrderVO(TOrder order, Passenger passenger, Train train,
                                     Seat seat, Carriage carriage) {
        OrderVO vo = new OrderVO();
        vo.setOrderId(order.getOrderId());
        vo.setUserId(order.getUserId());
        vo.setPassengerId(order.getPassengerId());
        vo.setPassengerName(passenger != null ? passenger.getRealName() : null);
        vo.setTrainId(order.getTrainId());
        vo.setTrainNo(train != null ? train.getTrainNo() : null);
        vo.setSeatId(order.getSeatId());
        vo.setSeatNo(seat != null ? seat.getSeatNo() : null);
        vo.setCarriageLevel(carriage != null ? carriage.getCarriageLevel() : null);
        vo.setTicketPrice(order.getTicketPrice());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setCreateTime(order.getCreateTime());
        vo.setPayDeadline(order.getCreateTime() != null
                ? order.getCreateTime().plusMinutes(30) : null);
        return vo;
    }
}
