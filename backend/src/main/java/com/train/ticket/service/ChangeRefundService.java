package com.train.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.train.ticket.common.Constants;
import com.train.ticket.dto.ChangeOrderDTO;
import com.train.ticket.dto.OrderDetailVO;
import com.train.ticket.dto.OrderQueryDTO;
import com.train.ticket.dto.RefundResultVO;
import com.train.ticket.entity.*;
import com.train.ticket.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChangeRefundService {

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
    private ChangeRecordMapper changeRecordMapper;
    @Autowired
    private RefundRecordMapper refundRecordMapper;
    @Autowired
    private StationMapper stationMapper;
    @Autowired
    private TrainStationRouteMapper routeMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 订单多条件筛选查询 ====================

    /**
     * 用户端/管理员端通用订单查询
     */
    public List<OrderDetailVO> queryOrders(OrderQueryDTO dto) {
        // 先查符合条件的订单ID
        // 如果有trainNo/carriageNo/seatNo等关联条件，需要先反查
        LambdaQueryWrapper<TOrder> wrapper = new LambdaQueryWrapper<>();

        if (dto.getUserId() != null) {
            wrapper.eq(TOrder::getUserId, dto.getUserId());
        }
        if (dto.getOrderId() != null) {
            wrapper.eq(TOrder::getOrderId, dto.getOrderId());
        }
        if (dto.getOrderStatus() != null && !dto.getOrderStatus().isEmpty()) {
            wrapper.eq(TOrder::getOrderStatus, dto.getOrderStatus());
        }
        if (dto.getCreateTimeStart() != null && !dto.getCreateTimeStart().isEmpty()) {
            wrapper.ge(TOrder::getCreateTime, LocalDateTime.parse(dto.getCreateTimeStart() + " 00:00:00", FORMATTER));
        }
        if (dto.getCreateTimeEnd() != null && !dto.getCreateTimeEnd().isEmpty()) {
            wrapper.le(TOrder::getCreateTime, LocalDateTime.parse(dto.getCreateTimeEnd() + " 23:59:59", FORMATTER));
        }

        // 关联条件：trainNo → trainId
        if (dto.getTrainNo() != null && !dto.getTrainNo().isEmpty()) {
            List<Train> trains = trainMapper.selectList(
                    new LambdaQueryWrapper<Train>().like(Train::getTrainNo, dto.getTrainNo()));
            if (trains.isEmpty()) return Collections.emptyList();
            List<Long> trainIds = trains.stream().map(Train::getTrainId).toList();
            wrapper.in(TOrder::getTrainId, trainIds);
        }

        // 关联条件：startStationId/endStationId → trainId
        if (dto.getStartStationId() != null || dto.getEndStationId() != null) {
            LambdaQueryWrapper<Train> trainWrapper = new LambdaQueryWrapper<>();
            if (dto.getStartStationId() != null) {
                trainWrapper.eq(Train::getStartStationId, dto.getStartStationId());
            }
            if (dto.getEndStationId() != null) {
                trainWrapper.eq(Train::getEndStationId, dto.getEndStationId());
            }
            List<Train> trains = trainMapper.selectList(trainWrapper);
            if (trains.isEmpty()) return Collections.emptyList();
            List<Long> trainIds = trains.stream().map(Train::getTrainId).toList();
            wrapper.in(TOrder::getTrainId, trainIds);
        }

        // 关联条件：travelDate → train.depart_datetime 的日期
        if (dto.getTravelDate() != null && !dto.getTravelDate().isEmpty()) {
            LocalDate travelDate = LocalDate.parse(dto.getTravelDate());
            LocalDateTime dayStart = travelDate.atStartOfDay();
            LocalDateTime dayEnd = travelDate.plusDays(1).atStartOfDay();
            List<Train> trains = trainMapper.selectList(
                    new LambdaQueryWrapper<Train>()
                            .ge(Train::getDepartDatetime, dayStart)
                            .lt(Train::getDepartDatetime, dayEnd));
            if (trains.isEmpty()) return Collections.emptyList();
            List<Long> trainIds = trains.stream().map(Train::getTrainId).toList();
            wrapper.in(TOrder::getTrainId, trainIds);
        }

        // 关联条件：seatNo/carriageNo → seatId
        if ((dto.getSeatNo() != null && !dto.getSeatNo().isEmpty())
                || (dto.getCarriageNo() != null && !dto.getCarriageNo().isEmpty())) {
            LambdaQueryWrapper<Seat> seatWrapper = new LambdaQueryWrapper<>();
            if (dto.getSeatNo() != null && !dto.getSeatNo().isEmpty()) {
                seatWrapper.like(Seat::getSeatNo, dto.getSeatNo());
            }
            if (dto.getCarriageNo() != null && !dto.getCarriageNo().isEmpty()) {
                // 先查车厢
                List<Carriage> carriages = carriageMapper.selectList(
                        new LambdaQueryWrapper<Carriage>().like(Carriage::getCarriageNo, dto.getCarriageNo()));
                if (carriages.isEmpty()) return Collections.emptyList();
                List<Long> carriageIds = carriages.stream().map(Carriage::getCarriageId).toList();
                seatWrapper.in(Seat::getCarriageId, carriageIds);
            }
            List<Seat> seats = seatMapper.selectList(seatWrapper);
            if (seats.isEmpty()) return Collections.emptyList();
            List<Long> seatIds = seats.stream().map(Seat::getSeatId).toList();
            wrapper.in(TOrder::getSeatId, seatIds);
        }

        wrapper.orderByDesc(TOrder::getCreateTime);
        List<TOrder> orders = orderMapper.selectList(wrapper);

        return orders.stream().map(this::convertToDetailVO).toList();
    }

    // ==================== 改签 ====================

    /**
     * 改签业务
     * 规则：
     * 1. 不更换乘客
     * 2. 不能修改起点终点
     * 3. 只能改签至有余票的车次
     * 4. 席位升降档，差价多退少补
     * 5. 发车前可改签；发车后仅允许改当日后续车次
     * 6. 原始订单仅可改签一次
     * 7. 改签后原座位回收释放
     */
    @Transactional
    public OrderDetailVO changeOrder(ChangeOrderDTO dto) {
        // 1. 查原订单
        TOrder oldOrder = orderMapper.selectById(dto.getOldOrderId());
        if (oldOrder == null) {
            throw new RuntimeException("原始订单不存在");
        }

        // 2. 校验订单状态：必须是已支付
        if (!Constants.ORDER_PAID.equals(oldOrder.getOrderStatus())) {
            throw new RuntimeException("仅已支付订单可改签");
        }

        // 3. 校验是否已改签过（一个原始订单仅可改签一次）
        // 如果是改签后的订单(CHANGED状态)再改签，也不允许
        Long changeCount = changeRecordMapper.selectCount(
                new LambdaQueryWrapper<ChangeRecord>()
                        .eq(ChangeRecord::getOldOrderId, dto.getOldOrderId()));
        if (changeCount > 0) {
            throw new RuntimeException("该订单已改签过，不可再次改签");
        }

        // 4. 查原车次和新车次
        Train oldTrain = trainMapper.selectById(oldOrder.getTrainId());
        if (oldTrain == null) {
            throw new RuntimeException("原车次不存在");
        }
        Train newTrain = trainMapper.selectById(dto.getNewTrainId());
        if (newTrain == null) {
            throw new RuntimeException("新车次不存在");
        }

        // 5. 校验起止站点不可变更
        if (!oldTrain.getStartStationId().equals(newTrain.getStartStationId())
                || !oldTrain.getEndStationId().equals(newTrain.getEndStationId())) {
            throw new RuntimeException("改签不可修改起点终点");
        }

        // 6. 校验发车时间规则
        LocalDateTime now = LocalDateTime.now();
        boolean oldTrainDeparted = oldTrain.getDepartDatetime().isBefore(now);

        if (oldTrainDeparted) {
            // 原车次已发车：仅允许改签当日后续车次
            if (!newTrain.getDepartDatetime().toLocalDate().equals(oldTrain.getDepartDatetime().toLocalDate())) {
                throw new RuntimeException("已发车订单仅可改签当日后续车次");
            }
            if (newTrain.getDepartDatetime().isBefore(now)) {
                throw new RuntimeException("已发车订单仅可改签当日后续车次");
            }
        } else {
            // 原车次未发车：可改签，但新车次也不能是已发车的
            if (newTrain.getDepartDatetime().isBefore(now)) {
                throw new RuntimeException("不可改签至已发车车次");
            }
        }

        // 7. 校验新车次售票状态
        if (newTrain.getSaleStatus() == null || newTrain.getSaleStatus() == Constants.SALE_STATUS_CLOSED) {
            throw new RuntimeException("新车次尚未开放售票");
        }

        // 8. 校验新座位
        Seat newSeat = seatMapper.selectById(dto.getNewSeatId());
        if (newSeat == null) {
            throw new RuntimeException("新座位不存在");
        }
        Carriage newCarriage = carriageMapper.selectById(newSeat.getCarriageId());
        if (newCarriage == null || !newCarriage.getTrainId().equals(dto.getNewTrainId())) {
            throw new RuntimeException("新座位不属于该车次");
        }

        // 9. 校验新座位是否被占用（有余票）
        Long occupied = orderMapper.selectCount(
                new LambdaQueryWrapper<TOrder>()
                        .eq(TOrder::getTrainId, dto.getNewTrainId())
                        .eq(TOrder::getSeatId, dto.getNewSeatId())
                        .in(TOrder::getOrderStatus,
                                Constants.ORDER_PENDING, Constants.ORDER_PAID));
        if (occupied > 0) {
            throw new RuntimeException("新座位已被占用，无法改签");
        }

        // 10. 计算差价（多退少补）
        BigDecimal oldPrice = oldOrder.getTicketPrice();
        BigDecimal newPrice = getPriceByLevel(newCarriage.getCarriageLevel());
        BigDecimal priceDiff = newPrice.subtract(oldPrice);

        // 11. 原订单状态改为已改签，释放原座位
        oldOrder.setOrderStatus(Constants.ORDER_CHANGED);
        orderMapper.updateById(oldOrder);

        // 12. 创建新订单
        TOrder newOrder = new TOrder();
        newOrder.setUserId(oldOrder.getUserId());
        newOrder.setPassengerId(oldOrder.getPassengerId()); // 不更换乘客
        newOrder.setTrainId(dto.getNewTrainId());
        newOrder.setSeatId(dto.getNewSeatId());
        newOrder.setTicketPrice(newPrice);
        newOrder.setOrderStatus(Constants.ORDER_PAID); // 改签后直接为已支付
        newOrder.setCreateTime(LocalDateTime.now());
        orderMapper.insert(newOrder);

        // 13. 生成改签记录
        ChangeRecord record = new ChangeRecord();
        record.setOldOrderId(oldOrder.getOrderId());
        record.setNewOrderId(newOrder.getOrderId());
        record.setChangeTime(LocalDateTime.now());
        record.setPriceDiff(priceDiff);
        changeRecordMapper.insert(record);

        return convertToDetailVO(newOrder);
    }

    // ==================== 退票 ====================

    /**
     * 退票业务
     * 规则：
     * 1. 未发车可退票，已发车不可退票
     * 2. 已改签订单允许退票
     * 3. 状态流转：已支付/已改签 → 退票中 → 已退票
     * 4. 模拟时间阶梯手续费
     * 5. 退票后座位释放
     */
    @Transactional
    public RefundResultVO refundOrder(Long orderId) {
        TOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 校验状态：已支付或已改签可退票
        String status = order.getOrderStatus();
        if (!Constants.ORDER_PAID.equals(status) && !Constants.ORDER_CHANGED.equals(status)) {
            throw new RuntimeException("仅已支付或已改签订单可退票");
        }

        Train train = trainMapper.selectById(order.getTrainId());
        if (train == null) {
            throw new RuntimeException("车次不存在");
        }

        // 校验是否已发车
        LocalDateTime now = LocalDateTime.now();
        if (train.getDepartDatetime().isBefore(now)) {
            throw new RuntimeException("车次已发车，不可退票");
        }

        // 计算阶梯手续费
        BigDecimal refundFee = calculateRefundFee(train.getDepartDatetime(), order.getTicketPrice());

        // 状态流转：已支付/已改签 → 退票中 → 已退票
        order.setOrderStatus(Constants.ORDER_REFUNDING);
        orderMapper.updateById(order);

        // 立即转为已退票（模拟退票流程完成）
        order.setOrderStatus(Constants.ORDER_REFUNDED);
        orderMapper.updateById(order);

        // 生成退票记录
        RefundRecord record = new RefundRecord();
        record.setOrderId(orderId);
        record.setRefundTime(now);
        record.setRefundFee(refundFee);
        refundRecordMapper.insert(record);

        RefundResultVO vo = new RefundResultVO();
        vo.setOrderId(orderId);
        vo.setRefundFee(refundFee);
        vo.setRefundAmount(order.getTicketPrice().subtract(refundFee));
        return vo;
    }

    /**
     * 退票手续费阶梯计算：
     * - 发车前>48小时：5%
     * - 发车前24~48小时：10%
     * - 发车前<24小时：20%
     */
    private BigDecimal calculateRefundFee(LocalDateTime departTime, BigDecimal ticketPrice) {
        LocalDateTime now = LocalDateTime.now();
        long hoursUntilDepart = Duration.between(now, departTime).toHours();

        BigDecimal feeRate;
        if (hoursUntilDepart > 48) {
            feeRate = new BigDecimal("0.05");
        } else if (hoursUntilDepart > 24) {
            feeRate = new BigDecimal("0.10");
        } else {
            feeRate = new BigDecimal("0.20");
        }

        return ticketPrice.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
    }

    // ==================== 用户主动取消订单 ====================

    /**
     * 用户主动取消待支付订单，释放座位
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        TOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!Constants.ORDER_PENDING.equals(order.getOrderStatus())) {
            throw new RuntimeException("仅待支付订单可取消");
        }
        order.setOrderStatus(Constants.ORDER_CANCELLED);
        orderMapper.updateById(order);
    }

    // ==================== 工具方法 ====================

    private BigDecimal getPriceByLevel(String level) {
        if ("商务".equals(level)) return new BigDecimal("500.00");
        if ("一等".equals(level)) return new BigDecimal("300.00");
        if ("二等".equals(level)) return new BigDecimal("150.00");
        return new BigDecimal("100.00");
    }

    public OrderDetailVO convertToDetailVO(TOrder order) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrderId(order.getOrderId());
        vo.setUserId(order.getUserId());
        vo.setPassengerId(order.getPassengerId());
        vo.setTrainId(order.getTrainId());
        vo.setSeatId(order.getSeatId());
        vo.setTicketPrice(order.getTicketPrice());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setCreateTime(order.getCreateTime());
        vo.setPayDeadline(order.getCreateTime() != null
                ? order.getCreateTime().plusMinutes(30) : null);

        // 用户名
        TUser user = userMapper.selectById(order.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
        }

        // 乘车人
        Passenger passenger = passengerMapper.selectById(order.getPassengerId());
        if (passenger != null) {
            vo.setPassengerName(passenger.getRealName());
            vo.setPassengerIdCard(passenger.getIdCard());
        }

        // 车次
        Train train = trainMapper.selectById(order.getTrainId());
        if (train != null) {
            vo.setTrainNo(train.getTrainNo());
            vo.setTrainType(train.getTrainType());
            vo.setDepartTime(train.getDepartDatetime() != null
                    ? train.getDepartDatetime().format(FORMATTER) : null);
            vo.setArriveTime(train.getArriveDatetime() != null
                    ? train.getArriveDatetime().format(FORMATTER) : null);
            vo.setStartStationId(train.getStartStationId());
            vo.setEndStationId(train.getEndStationId());

            Station startStation = stationMapper.selectById(train.getStartStationId());
            Station endStation = stationMapper.selectById(train.getEndStationId());
            vo.setStartStationName(startStation != null ? startStation.getStationName() : null);
            vo.setStartCity(startStation != null ? startStation.getCity() : null);
            vo.setEndStationName(endStation != null ? endStation.getStationName() : null);
            vo.setEndCity(endStation != null ? endStation.getCity() : null);
        }

        // 座位和车厢
        Seat seat = seatMapper.selectById(order.getSeatId());
        if (seat != null) {
            vo.setSeatNo(seat.getSeatNo());
            Carriage carriage = carriageMapper.selectById(seat.getCarriageId());
            if (carriage != null) {
                vo.setCarriageId(carriage.getCarriageId());
                vo.setCarriageNo(carriage.getCarriageNo());
                vo.setCarriageLevel(carriage.getCarriageLevel());
            }
        }

        return vo;
    }
}
