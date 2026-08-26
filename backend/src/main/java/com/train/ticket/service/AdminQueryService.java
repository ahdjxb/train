package com.train.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.train.ticket.common.Constants;
import com.train.ticket.dto.OrderDetailVO;
import com.train.ticket.dto.OrderQueryDTO;
import com.train.ticket.dto.TicketStatsVO;
import com.train.ticket.entity.*;
import com.train.ticket.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminQueryService {

    @Autowired
    private TOrderMapper orderMapper;
    @Autowired
    private TrainMapper trainMapper;
    @Autowired
    private CarriageMapper carriageMapper;
    @Autowired
    private SeatMapper seatMapper;
    @Autowired
    private TUserMapper userMapper;
    @Autowired
    private StationMapper stationMapper;
    @Autowired
    private ChangeRefundService changeRefundService;

    /**
     * 查询全部订单（管理员，仅查看）
     */
    public List<OrderDetailVO> queryAllOrders(OrderQueryDTO dto) {
        // 不设userId，查询全部
        dto.setUserId(null);
        return changeRefundService.queryOrders(dto);
    }

    /**
     * 查询全部车票（管理员）
     * 返回所有已支付/已改签/已完成的车票订单
     */
    public List<OrderDetailVO> queryAllTickets(OrderQueryDTO dto) {
        List<OrderDetailVO> allOrders = changeRefundService.queryOrders(dto);
        // 只返回有车票的（已支付、已改签、已完成）
        return allOrders.stream()
                .filter(o -> Constants.ORDER_PAID.equals(o.getOrderStatus())
                        || Constants.ORDER_CHANGED.equals(o.getOrderStatus())
                        || Constants.ORDER_FINISHED.equals(o.getOrderStatus()))
                .collect(Collectors.toList());
    }

    /**
     * 车票统计：余量、出票量、退票量、异常座位检测
     */
    public TicketStatsVO getTicketStats() {
        TicketStatsVO vo = new TicketStatsVO();

        // 总车次数
        long totalTrains = trainMapper.selectCount(null);
        vo.setTotalTrains((int) totalTrains);

        // 总座位数
        long totalSeats = seatMapper.selectCount(null);
        vo.setTotalSeats((int) totalSeats);

        // 各状态订单数
        long paidCount = orderMapper.selectCount(
                new LambdaQueryWrapper<TOrder>().eq(TOrder::getOrderStatus, Constants.ORDER_PAID));
        long changedCount = orderMapper.selectCount(
                new LambdaQueryWrapper<TOrder>().eq(TOrder::getOrderStatus, Constants.ORDER_CHANGED));
        long pendingCount = orderMapper.selectCount(
                new LambdaQueryWrapper<TOrder>().eq(TOrder::getOrderStatus, Constants.ORDER_PENDING));
        long refundedCount = orderMapper.selectCount(
                new LambdaQueryWrapper<TOrder>().eq(TOrder::getOrderStatus, Constants.ORDER_REFUNDED));
        long finishedCount = orderMapper.selectCount(
                new LambdaQueryWrapper<TOrder>().eq(TOrder::getOrderStatus, Constants.ORDER_FINISHED));

        // 出票量 = 已支付 + 已改签 + 已完成
        vo.setIssuedCount((int) (paidCount + changedCount + finishedCount));
        // 退票量
        vo.setRefundCount((int) refundedCount);
        // 待支付占用
        vo.setPendingCount((int) pendingCount);
        // 已售出 = 已支付 + 已改签
        vo.setSoldCount((int) (paidCount + changedCount));
        // 可售余票 = 总座位 - 已售出 - 待支付占用
        vo.setAvailableCount((int) (totalSeats - paidCount - changedCount - pendingCount));

        // 按车型统计出票
        Map<String, Integer> statsByType = new LinkedHashMap<>();
        statsByType.put("高铁", 0);
        statsByType.put("普通", 0);
        List<TOrder> activeOrders = orderMapper.selectList(
                new LambdaQueryWrapper<TOrder>()
                        .in(TOrder::getOrderStatus,
                                Constants.ORDER_PAID,
                                Constants.ORDER_CHANGED,
                                Constants.ORDER_FINISHED));
        for (TOrder order : activeOrders) {
            Train train = trainMapper.selectById(order.getTrainId());
            if (train != null) {
                String type = train.getTrainType();
                statsByType.put(type, statsByType.getOrDefault(type, 0) + 1);
            }
        }
        vo.setStatsByType(statsByType);

        // 异常座位检测：同一车次同一座位被多个有效订单占用
        List<TicketStatsVO.DuplicateSeatVO> dupList = detectDuplicateSeats();
        vo.setDuplicateSeats(dupList);

        // 各车次车票统计列表
        vo.setTrainStatsList(buildTrainStatsList());

        return vo;
    }

    /**
     * 构建各车次车票统计列表
     */
    private List<TicketStatsVO.TrainTicketStatsVO> buildTrainStatsList() {
        List<Train> trains = trainMapper.selectList(null);
        List<TicketStatsVO.TrainTicketStatsVO> list = new ArrayList<>();
        for (Train train : trains) {
            TicketStatsVO.TrainTicketStatsVO stats = new TicketStatsVO.TrainTicketStatsVO();
            stats.setTrainId(train.getTrainId());
            stats.setTrainNo(train.getTrainNo());
            stats.setTrainType(train.getTrainType());
            stats.setDepartDatetime(train.getDepartDatetime() != null
                    ? train.getDepartDatetime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    : null);

            Station startSt = stationMapper.selectById(train.getStartStationId());
            Station endSt = stationMapper.selectById(train.getEndStationId());
            stats.setStartStationName(startSt != null ? startSt.getStationName() : null);
            stats.setEndStationName(endSt != null ? endSt.getStationName() : null);

            // 总座位数
            List<Carriage> carriages = carriageMapper.selectList(
                    new LambdaQueryWrapper<Carriage>().eq(Carriage::getTrainId, train.getTrainId()));
            int totalSeats = 0;
            for (Carriage c : carriages) {
                totalSeats += seatMapper.selectCount(
                        new LambdaQueryWrapper<Seat>().eq(Seat::getCarriageId, c.getCarriageId())).intValue();
            }
            stats.setTotalSeats(totalSeats);

            // 各状态订单数
            long issued = orderMapper.selectCount(new LambdaQueryWrapper<TOrder>()
                    .eq(TOrder::getTrainId, train.getTrainId())
                    .in(TOrder::getOrderStatus, Constants.ORDER_PAID, Constants.ORDER_CHANGED, Constants.ORDER_FINISHED));
            long pending = orderMapper.selectCount(new LambdaQueryWrapper<TOrder>()
                    .eq(TOrder::getTrainId, train.getTrainId())
                    .eq(TOrder::getOrderStatus, Constants.ORDER_PENDING));
            long refunded = orderMapper.selectCount(new LambdaQueryWrapper<TOrder>()
                    .eq(TOrder::getTrainId, train.getTrainId())
                    .eq(TOrder::getOrderStatus, Constants.ORDER_REFUNDED));

            stats.setIssuedCount((int) issued);
            stats.setPendingCount((int) pending);
            stats.setRefundCount((int) refunded);
            stats.setAvailableCount(totalSeats - (int) issued - (int) pending);

            list.add(stats);
        }
        return list;
    }

    /**
     * 检测座位重复售卖异常
     * 正常情况下一个(trainId, seatId)组合只能有一个有效订单（PENDING/PAID）
     */
    private List<TicketStatsVO.DuplicateSeatVO> detectDuplicateSeats() {
        List<TOrder> activeOrders = orderMapper.selectList(
                new LambdaQueryWrapper<TOrder>()
                        .in(TOrder::getOrderStatus,
                                Constants.ORDER_PENDING,
                                Constants.ORDER_PAID));

        // 按 (trainId, seatId) 分组统计
        Map<String, List<TOrder>> groupMap = activeOrders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getTrainId() + "_" + o.getSeatId()));

        List<TicketStatsVO.DuplicateSeatVO> dupList = new ArrayList<>();
        for (Map.Entry<String, List<TOrder>> entry : groupMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                // 异常：同一座位被多个有效订单占用
                TOrder firstOrder = entry.getValue().get(0);
                Seat seat = seatMapper.selectById(firstOrder.getSeatId());
                Train train = trainMapper.selectById(firstOrder.getTrainId());

                TicketStatsVO.DuplicateSeatVO dup = new TicketStatsVO.DuplicateSeatVO();
                dup.setSeatId(firstOrder.getSeatId());
                dup.setSeatNo(seat != null ? seat.getSeatNo() : null);
                dup.setTrainId(firstOrder.getTrainId());
                dup.setTrainNo(train != null ? train.getTrainNo() : null);
                dup.setActiveOrderCount(entry.getValue().size());
                dupList.add(dup);
            }
        }
        return dupList;
    }

    // ==================== 用户管理 ====================

    /**
     * 查询全部普通用户
     */
    public List<TUser> listNormalUsers(String username, Integer isLock) {
        LambdaQueryWrapper<TUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TUser::getRole, Constants.ROLE_NORMAL);
        if (username != null && !username.isEmpty()) {
            wrapper.like(TUser::getUsername, username);
        }
        if (isLock != null) {
            wrapper.eq(TUser::getIsLock, isLock);
        }
        wrapper.orderByDesc(TUser::getUserId);
        List<TUser> users = userMapper.selectList(wrapper);
        // 清除密码
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    /**
     * 锁定/解锁用户
     */
    public void setUserLockStatus(Long userId, Integer isLock) {
        TUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!Constants.ROLE_NORMAL.equals(user.getRole())) {
            throw new RuntimeException("不可操作管理员账号");
        }
        user.setIsLock(isLock);
        userMapper.updateById(user);
    }
}
