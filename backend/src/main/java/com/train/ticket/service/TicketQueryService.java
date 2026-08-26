package com.train.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.train.ticket.common.Constants;
import com.train.ticket.dto.*;
import com.train.ticket.entity.*;
import com.train.ticket.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TicketQueryService {

    @Autowired
    private TrainMapper trainMapper;
    @Autowired
    private TrainTicketMapper trainTicketMapper;
    @Autowired
    private TrainStationRouteMapper routeMapper;
    @Autowired
    private StationMapper stationMapper;
    @Autowired
    private CarriageMapper carriageMapper;
    @Autowired
    private SeatMapper seatMapper;
    @Autowired
    private TOrderMapper orderMapper;

    private static final int MIN_TRANSFER_MINUTES = 15;

    /**
     * 车票查询入口：直达优先，无直达才查中转
     */
    public Map<String, Object> searchTickets(TicketQueryDTO dto) {
        Map<String, Object> result = new HashMap<>();
        result.put("direct", Collections.emptyList());
        result.put("transfer", Collections.emptyList());

        if (dto.getStartCity() == null || dto.getEndCity() == null || dto.getDate() == null) {
            return result;
        }
        if (dto.getStartCity().equals(dto.getEndCity())) {
            return result;
        }

        LocalDate queryDate = LocalDate.parse(dto.getDate());

        // 1. 先查直达
        List<TicketResultVO> directList = queryDirectByCity(dto.getStartCity(), dto.getEndCity(), queryDate,
                dto.getTrainType(), dto.getStartStationId(), dto.getEndStationId());
        result.put("direct", directList);

        // 2. 仅当用户勾选全部或中转时才查中转
        String filterType = dto.getFilterType();
        boolean shouldQueryTransfer = "transfer".equals(filterType) ||
                ("".equals(filterType) || filterType == null) && directList.isEmpty();
        if (shouldQueryTransfer) {
            List<TransferResultVO> transferList = queryTransfer(dto.getStartCity(), dto.getEndCity(), queryDate, dto.getTrainType());
            result.put("transfer", transferList);
        }

        return result;
    }

    // ==================== 直达查询 ====================

    private List<TicketResultVO> queryDirectByCity(String startCity, String endCity, LocalDate queryDate,
                                                    String trainType, Long startStationId, Long endStationId) {
        List<Train> trains;
        if (startStationId != null && endStationId != null) {
            trains = trainTicketMapper.searchDirectTicket(startStationId, endStationId, queryDate);
        } else {
            trains = trainTicketMapper.searchDirectTicketByCity(startCity, endCity, queryDate);
        }

        if (trainType != null && !trainType.isEmpty()) {
            trains = trains.stream().filter(t -> trainType.equals(t.getTrainType())).collect(Collectors.toList());
        }

        List<TicketResultVO> voList = new ArrayList<>();
        for (Train train : trains) {
            TicketResultVO vo = buildDirectTicketVO(train, startCity, endCity, startStationId, endStationId);
            voList.add(vo);
        }
        return voList;
    }

    private TicketResultVO buildDirectTicketVO(Train train, String startCity, String endCity,
                                               Long startStationId, Long endStationId) {
        TicketResultVO vo = new TicketResultVO();
        vo.setTrainId(train.getTrainId());
        vo.setTrainNo(train.getTrainNo());
        vo.setTrainType(train.getTrainType());
        vo.setDepartDatetime(formatDateTime(train.getDepartDatetime()));
        vo.setArriveDatetime(formatDateTime(train.getArriveDatetime()));
        vo.setSaleStatus(train.getSaleStatus());

        if (train.getDepartDatetime() != null && train.getArriveDatetime() != null) {
            Duration d = Duration.between(train.getDepartDatetime(), train.getArriveDatetime());
            vo.setDuration(String.format("%02d:%02d", d.toHours(), d.toMinutesPart()));
        }

        List<TrainStationRoute> routes = routeMapper.selectList(
                new LambdaQueryWrapper<TrainStationRoute>()
                        .eq(TrainStationRoute::getTrainId, train.getTrainId())
                        .orderByAsc(TrainStationRoute::getSort));

        if (startStationId != null) {
            Station s = stationMapper.selectById(startStationId);
            vo.setStartStationName(s != null ? s.getStationName() : null);
            for (TrainStationRoute r : routes) {
                if (r.getStationId().equals(startStationId)) {
                    vo.setDepartTime(formatDateTime(r.getDepartDatetime()));
                    break;
                }
            }
        } else {
            for (TrainStationRoute r : routes) {
                Station s = stationMapper.selectById(r.getStationId());
                if (s != null && s.getCity().equals(startCity)) {
                    vo.setStartStationName(s.getStationName());
                    vo.setDepartTime(formatDateTime(r.getDepartDatetime()));
                    break;
                }
            }
        }

        if (endStationId != null) {
            Station s = stationMapper.selectById(endStationId);
            vo.setEndStationName(s != null ? s.getStationName() : null);
            for (TrainStationRoute r : routes) {
                if (r.getStationId().equals(endStationId)) {
                    vo.setArriveTime(formatDateTime(r.getArriveDatetime()));
                    break;
                }
            }
        } else {
            for (TrainStationRoute r : routes) {
                Station s = stationMapper.selectById(r.getStationId());
                if (s != null && s.getCity().equals(endCity)) {
                    vo.setEndStationName(s.getStationName());
                    vo.setArriveTime(formatDateTime(r.getArriveDatetime()));
                    break;
                }
            }
        }

        List<TicketResultVO.SeatAvailabilityVO> seatList = getSeatAvailability(train.getTrainId());
        vo.setSeatList(seatList);
        vo.setHasTicket(seatList.stream().anyMatch(s -> s.getAvailableCount() > 0));

        return vo;
    }

    private List<TicketResultVO.SeatAvailabilityVO> getSeatAvailability(Long trainId) {
        List<Carriage> carriages = carriageMapper.selectList(
                new LambdaQueryWrapper<Carriage>().eq(Carriage::getTrainId, trainId));

        Map<String, List<Seat>> seatsByLevel = new LinkedHashMap<>();
        for (Carriage c : carriages) {
            seatsByLevel.computeIfAbsent(c.getCarriageLevel(), k -> new ArrayList<>());
            List<Seat> seats = seatMapper.selectList(
                    new LambdaQueryWrapper<Seat>().eq(Seat::getCarriageId, c.getCarriageId()));
            seatsByLevel.get(c.getCarriageLevel()).addAll(seats);
        }

        List<TicketResultVO.SeatAvailabilityVO> result = new ArrayList<>();
        for (Map.Entry<String, List<Seat>> entry : seatsByLevel.entrySet()) {
            TicketResultVO.SeatAvailabilityVO sa = new TicketResultVO.SeatAvailabilityVO();
            sa.setCarriageLevel(entry.getKey());
            sa.setTotalCount(entry.getValue().size());
            int available = 0;
            for (Seat seat : entry.getValue()) {
                Long occupied = orderMapper.selectCount(
                        new LambdaQueryWrapper<TOrder>()
                                .eq(TOrder::getTrainId, trainId)
                                .eq(TOrder::getSeatId, seat.getSeatId())
                                .in(TOrder::getOrderStatus, Constants.ORDER_PENDING, Constants.ORDER_PAID));
                if (occupied == 0) available++;
            }
            sa.setAvailableCount(available);
            sa.setPrice(getPriceByLevel(entry.getKey()));
            result.add(sa);
        }
        return result;
    }

    // ==================== 中转查询（思路B） ====================

    private List<TransferResultVO> queryTransfer(String startCity, String endCity, LocalDate queryDate, String trainType) {
        // 步骤1：查S可以到达的所有中转站T
        List<StoTTransferDTO> sToTList = trainTicketMapper.getCanArriveTransferStation(startCity, endCity, queryDate);
        if (sToTList == null || sToTList.isEmpty()) return Collections.emptyList();

        if (trainType != null && !trainType.isEmpty()) {
            sToTList = sToTList.stream().filter(s -> {
                Train t = trainMapper.selectById(s.getTrainId1());
                return t != null && trainType.equals(t.getTrainType());
            }).collect(Collectors.toList());
        }

        Set<Long> tIdSet = sToTList.stream().map(StoTTransferDTO::getTransferStationId).collect(Collectors.toSet());
        if (tIdSet.isEmpty()) return Collections.emptyList();

        // 步骤2：批量查询T可以到达终点E的行程
        List<TtoETransferDTO> tToEList = trainTicketMapper.getTransferToEnd(new ArrayList<>(tIdSet), endCity, queryDate);
        if (tToEList == null || tToEList.isEmpty()) return Collections.emptyList();

        if (trainType != null && !trainType.isEmpty()) {
            tToEList = tToEList.stream().filter(s -> {
                Train t = trainMapper.selectById(s.getTrainId2());
                return t != null && trainType.equals(t.getTrainType());
            }).collect(Collectors.toList());
        }

        // 步骤3：内存匹配
        Map<Long, List<TtoETransferDTO>> tToEGroup = tToEList.stream()
                .collect(Collectors.groupingBy(TtoETransferDTO::getTransferStationId));

        List<TransferResultVO> result = new ArrayList<>();
        for (StoTTransferDTO sToT : sToTList) {
            List<TtoETransferDTO> matchList = tToEGroup.get(sToT.getTransferStationId());
            if (matchList == null) continue;

            for (TtoETransferDTO tToE : matchList) {
                LocalDateTime arrive = sToT.getArriveTimeTransfer();
                LocalDateTime depart = tToE.getDepartTimeTransfer();
                if (arrive.plusMinutes(MIN_TRANSFER_MINUTES).isBefore(depart)) {
                    TransferResultVO vo = buildTransferVO(sToT, tToE);
                    result.add(vo);
                }
            }
        }

        // 去重
        result = new ArrayList<>(result.stream()
                .collect(Collectors.toMap(
                        v -> v.getSegments().get(0).getTrainId() + "_" +
                             v.getSegments().get(1).getTrainId() + "_" +
                             v.getTransferStationId(),
                        v -> v, (a, b) -> a, LinkedHashMap::new))
                .values());

        result.sort(Comparator.comparing(TransferResultVO::getTotalMinutes));

        return result;
    }

    private TransferResultVO buildTransferVO(StoTTransferDTO sToT, TtoETransferDTO tToE) {
        TransferResultVO vo = new TransferResultVO();
        vo.setTransferCount(1);

        vo.setStationNames(Arrays.asList(
                sToT.getStartStationName(),
                sToT.getTransferStationName(),
                tToE.getEndStationName()));

        long totalMinutes = Duration.between(sToT.getDepartTime1(), tToE.getArriveTime2()).toMinutes();
        vo.setTotalMinutes(totalMinutes);
        vo.setTotalDuration(String.format("%02d:%02d", totalMinutes / 60, totalMinutes % 60));

        List<TransferResultVO.TransferSegment> segments = new ArrayList<>();

        TransferResultVO.TransferSegment seg1 = new TransferResultVO.TransferSegment();
        seg1.setTrainId(sToT.getTrainId1());
        seg1.setTrainNo(sToT.getTrainNo1());
        Train train1 = trainMapper.selectById(sToT.getTrainId1());
        if (train1 != null) seg1.setTrainType(train1.getTrainType());
        seg1.setStartStationName(sToT.getStartStationName());
        seg1.setEndStationName(sToT.getTransferStationName());
        seg1.setDepartTime(formatDateTime(sToT.getDepartTime1()));
        seg1.setArriveTime(formatDateTime(sToT.getArriveTimeTransfer()));
        seg1.setHasTicket(checkTrainHasTicket(sToT.getTrainId1()));
        segments.add(seg1);

        TransferResultVO.TransferSegment seg2 = new TransferResultVO.TransferSegment();
        seg2.setTrainId(tToE.getTrainId2());
        seg2.setTrainNo(tToE.getTrainNo2());
        Train train2 = trainMapper.selectById(tToE.getTrainId2());
        if (train2 != null) seg2.setTrainType(train2.getTrainType());
        seg2.setStartStationName(tToE.getTransferStationName());
        seg2.setEndStationName(tToE.getEndStationName());
        seg2.setDepartTime(formatDateTime(tToE.getDepartTimeTransfer()));
        seg2.setArriveTime(formatDateTime(tToE.getArriveTime2()));
        seg2.setHasTicket(checkTrainHasTicket(tToE.getTrainId2()));
        segments.add(seg2);

        vo.setSegments(segments);
        vo.setTransferStationId(sToT.getTransferStationId());
        vo.setTransferStationName(sToT.getTransferStationName());
        vo.setHasTicket(seg1.getHasTicket() && seg2.getHasTicket());

        return vo;
    }

    private Boolean checkTrainHasTicket(Long trainId) {
        List<TicketResultVO.SeatAvailabilityVO> seats = getSeatAvailability(trainId);
        return seats.stream().anyMatch(s -> s.getAvailableCount() > 0);
    }

    // ==================== 城市和站点查询 ====================

    /**
     * 查询所有城市（去重）
     */
    public List<String> listCities() {
        List<Station> all = stationMapper.selectList(null);
        return all.stream().map(Station::getCity).distinct().sorted().collect(Collectors.toList());
    }

    /**
     * 查询某城市下的站点
     */
    public List<Station> listStationsByCity(String city) {
        return stationMapper.selectList(
                new LambdaQueryWrapper<Station>().eq(Station::getCity, city));
    }

    // ==================== 工具方法 ====================

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private java.math.BigDecimal getPriceByLevel(String level) {
        if (level == null) return java.math.BigDecimal.ZERO;
        return switch (level) {
            case "商务" -> new java.math.BigDecimal("1999.00");
            case "一等" -> new java.math.BigDecimal("999.00");
            case "二等" -> new java.math.BigDecimal("599.00");
            default -> new java.math.BigDecimal("199.00");
        };
    }
}
