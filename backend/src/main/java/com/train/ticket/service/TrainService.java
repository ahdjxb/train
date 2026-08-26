package com.train.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.train.ticket.common.Constants;
import com.train.ticket.dto.TrainDTO;
import com.train.ticket.dto.TrainVO;
import com.train.ticket.entity.Station;
import com.train.ticket.entity.Train;
import com.train.ticket.entity.TrainStationRoute;
import com.train.ticket.mapper.StationMapper;
import com.train.ticket.mapper.TrainMapper;
import com.train.ticket.mapper.TrainStationRouteMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class TrainService {

    @Autowired
    private TrainMapper trainMapper;
    @Autowired
    private TrainStationRouteMapper routeMapper;
    @Autowired
    private StationMapper stationMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 计算售票开放时间：发车前15天16:00
     */
    public LocalDateTime calculateSaleOpenTime(LocalDateTime departDatetime) {
        return departDatetime.minusDays(15)
                .withHour(16)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    /**
     * 新增车次（含途经站点）
     */
    @Transactional
    public Train addTrain(TrainDTO dto) {
        Train train = new Train();
        BeanUtils.copyProperties(dto, train);
        if (train.getSaleStatus() == null) {
            train.setSaleStatus(Constants.SALE_STATUS_CLOSED);
        }
        trainMapper.insert(train);

        // 保存途经站点
        if (dto.getRouteList() != null) {
            for (TrainDTO.RouteItem item : dto.getRouteList()) {
                TrainStationRoute route = new TrainStationRoute();
                route.setTrainId(train.getTrainId());
                route.setStationId(item.getStationId());
                route.setSort(item.getSort());
                route.setArriveDatetime(item.getArriveDatetime());
                route.setDepartDatetime(item.getDepartDatetime());
                routeMapper.insert(route);
            }
        }
        return train;
    }

    /**
     * 修改车次（含途经站点，先删后增）
     * - 已发车：不可修改
     * - 不在售票窗口期且已关售：不可修改
     * - 正在售票(saleStatus=1)：只允许修改时间类字段和添加途经站点
     */
    @Transactional
    public void updateTrain(TrainDTO dto) {
        if (dto.getTrainId() == null) {
            throw new RuntimeException("车次ID不能为空");
        }
        Train train = trainMapper.selectById(dto.getTrainId());
        if (train == null) {
            throw new RuntimeException("车次不存在");
        }

        // 校验：已发车不可修改
        if (train.getDepartDatetime() != null && train.getDepartDatetime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("已发车车次不可更改");
        }

        LocalDateTime saleOpenTime = calculateSaleOpenTime(train.getDepartDatetime());
        LocalDateTime now = LocalDateTime.now();
        boolean inSaleWindow = now.isAfter(saleOpenTime) && now.isBefore(train.getDepartDatetime());

        // 校验：不在默认售票时间范围内且已手动关售，不可修改
        if (!inSaleWindow && train.getSaleStatus() != null && train.getSaleStatus() == -1) {
            throw new RuntimeException("不在默认售票时间范围内的车次不可更改");
        }

        if (inSaleWindow && train.getSaleStatus() != null && train.getSaleStatus() == Constants.SALE_STATUS_OPEN) {
            // 正在售票：只允许修改时间类字段（始发站发车时间、终点站到达时间、途经站点到达/发车时间），可添加途经站点
            // 不修改车次号、车型、始发站、终点站、日期
            if (dto.getDepartDatetime() != null) {
                train.setDepartDatetime(dto.getDepartDatetime());
            }
            if (dto.getArriveDatetime() != null) {
                train.setArriveDatetime(dto.getArriveDatetime());
            }

            // 保留原途经站点，合并新添加的站点
            List<TrainStationRoute> existingRoutes = routeMapper.selectList(
                    new LambdaQueryWrapper<TrainStationRoute>()
                            .eq(TrainStationRoute::getTrainId, dto.getTrainId())
                            .orderByAsc(TrainStationRoute::getSort));
            // 用 Map 保存已有站点，按 stationId 索引
            java.util.Map<Long, TrainStationRoute> existingMap = new java.util.HashMap<>();
            int maxSort = 0;
            for (TrainStationRoute r : existingRoutes) {
                existingMap.put(r.getStationId(), r);
                if (r.getSort() != null && r.getSort() > maxSort) {
                    maxSort = r.getSort();
                }
            }

            if (dto.getRouteList() != null) {
                for (TrainDTO.RouteItem item : dto.getRouteList()) {
                    if (item.getStationId() != null && existingMap.containsKey(item.getStationId())) {
                        // 更新已有站点的时间
                        TrainStationRoute r = existingMap.get(item.getStationId());
                        if (item.getArriveDatetime() != null) {
                            r.setArriveDatetime(item.getArriveDatetime());
                        }
                        if (item.getDepartDatetime() != null) {
                            r.setDepartDatetime(item.getDepartDatetime());
                        }
                        routeMapper.updateById(r);
                    } else if (item.getStationId() != null) {
                        // 新增站点
                        TrainStationRoute route = new TrainStationRoute();
                        route.setTrainId(dto.getTrainId());
                        route.setStationId(item.getStationId());
                        route.setSort(item.getSort() != null ? item.getSort() : ++maxSort);
                        route.setArriveDatetime(item.getArriveDatetime());
                        route.setDepartDatetime(item.getDepartDatetime());
                        routeMapper.insert(route);
                    }
                }
            }
            trainMapper.updateById(train);
        } else {
            // 非售票期（未开售）：允许完整修改
            BeanUtils.copyProperties(dto, train);
            trainMapper.updateById(train);

            // 先删除原有途经站点
            routeMapper.delete(
                    new LambdaQueryWrapper<TrainStationRoute>()
                            .eq(TrainStationRoute::getTrainId, dto.getTrainId()));

            // 再插入新的途经站点
            if (dto.getRouteList() != null) {
                for (TrainDTO.RouteItem item : dto.getRouteList()) {
                    TrainStationRoute route = new TrainStationRoute();
                    route.setTrainId(dto.getTrainId());
                    route.setStationId(item.getStationId());
                    route.setSort(item.getSort());
                    route.setArriveDatetime(item.getArriveDatetime());
                    route.setDepartDatetime(item.getDepartDatetime());
                    routeMapper.insert(route);
                }
            }
        }
    }

    /**
     * 查询单个车次（含途经站点、站点名称）
     */
    public TrainVO getTrain(Long trainId) {
        Train train = trainMapper.selectById(trainId);
        if (train == null) {
            throw new RuntimeException("车次不存在");
        }
        return convertToVO(train);
    }

    /**
     * 查询全部车次列表（含已发车）
     */
    public List<TrainVO> listTrains(String trainNo, String trainType) {
        LambdaQueryWrapper<Train> wrapper = new LambdaQueryWrapper<>();
        if (trainNo != null && !trainNo.isEmpty()) {
            wrapper.like(Train::getTrainNo, trainNo);
        }
        if (trainType != null && !trainType.isEmpty()) {
            wrapper.eq(Train::getTrainType, trainType);
        }
        wrapper.orderByDesc(Train::getDepartDatetime);

        List<Train> trains = trainMapper.selectList(wrapper);
        List<TrainVO> voList = new ArrayList<>();
        for (Train train : trains) {
            autoUpdateSaleStatus(train);
            voList.add(convertToVO(train));
        }
        return voList;
    }

    /**
     * 改签查询：同始发城市、同终点城市、今日之后、正在售票的车次
     */
    public List<TrainVO> listTrainsForChange(String startCity, String endCity, String trainType) {
        // 查所有未发车且正在售票的车次
        LambdaQueryWrapper<Train> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(Train::getDepartDatetime, LocalDateTime.now());
        wrapper.eq(Train::getSaleStatus, Constants.SALE_STATUS_OPEN);
        if (trainType != null && !trainType.isEmpty()) {
            wrapper.eq(Train::getTrainType, trainType);
        }
        wrapper.orderByAsc(Train::getDepartDatetime);

        List<Train> trains = trainMapper.selectList(wrapper);
        List<TrainVO> voList = new ArrayList<>();
        for (Train train : trains) {
            // 检查始发站和终点站城市是否匹配
            Station startStation = stationMapper.selectById(train.getStartStationId());
            Station endStation = stationMapper.selectById(train.getEndStationId());
            if (startStation != null && endStation != null
                    && startStation.getCity().equals(startCity)
                    && endStation.getCity().equals(endCity)) {
                voList.add(convertToVO(train));
            }
        }
        return voList;
    }

    /**
     * 删除车次：只能删除未发车车次
     */
    @Transactional
    public void deleteTrain(Long trainId) {
        Train train = trainMapper.selectById(trainId);
        if (train == null) {
            throw new RuntimeException("车次不存在");
        }
        // 判断是否已发车
        if (train.getDepartDatetime() != null
                && train.getDepartDatetime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("已发车车次不可删除");
        }

        // 删除途经站点
        routeMapper.delete(
                new LambdaQueryWrapper<TrainStationRoute>()
                        .eq(TrainStationRoute::getTrainId, trainId));
        // 删除车次
        trainMapper.deleteById(trainId);
    }

    /**
     * 设置车次售票状态开关
     */
    public void setSaleStatus(Long trainId, Integer saleStatus) {
        Train train = trainMapper.selectById(trainId);
        if (train == null) {
            throw new RuntimeException("车次不存在");
        }
        train.setSaleStatus(saleStatus);
        trainMapper.updateById(train);
    }

    /**
     * 自动更新售票状态：
     * - 如果管理员已手动设置开售(1)，保持开售
     * - 如果管理员已手动设置关售(-1)，保持关售
     * - 默认(0/null)：发车前15天16点后自动开售，发车后自动关售
     */
    private void autoUpdateSaleStatus(Train train) {
        if (train.getDepartDatetime() == null) {
            return;
        }
        // 手动关售(-1)不自动覆盖
        if (train.getSaleStatus() != null && train.getSaleStatus() == -1) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        // 售票时间：发车前15天16点后自动开售
        LocalDateTime saleOpenTime = calculateSaleOpenTime(train.getDepartDatetime());

        if (now.isAfter(saleOpenTime) && now.isBefore(train.getDepartDatetime())) {
            // 售票窗口期内，自动开售
            if (train.getSaleStatus() == null || train.getSaleStatus() == 0) {
                train.setSaleStatus(Constants.SALE_STATUS_OPEN);
                trainMapper.updateById(train);
            }
        } else if (now.isAfter(train.getDepartDatetime())) {
            // 已发车，自动关售
            if (train.getSaleStatus() == null || train.getSaleStatus() == 0 || train.getSaleStatus() == 1) {
                train.setSaleStatus(-1);
                trainMapper.updateById(train);
            }
        }
    }

    /**
     * 转换为VO，补充站点名称和售票开放时间
     */
    private TrainVO convertToVO(Train train) {
        TrainVO vo = new TrainVO();
        vo.setTrainId(train.getTrainId());
        vo.setTrainNo(train.getTrainNo());
        vo.setDepartDatetime(train.getDepartDatetime() != null
                ? train.getDepartDatetime().format(FORMATTER) : null);
        vo.setArriveDatetime(train.getArriveDatetime() != null
                ? train.getArriveDatetime().format(FORMATTER) : null);
        vo.setStartStationId(train.getStartStationId());
        vo.setEndStationId(train.getEndStationId());
        vo.setTrainType(train.getTrainType());
        vo.setSaleStatus(train.getSaleStatus());

        // 站点名称
        if (train.getStartStationId() != null) {
            Station start = stationMapper.selectById(train.getStartStationId());
            if (start != null) {
                vo.setStartStationName(start.getStationName());
            }
        }
        if (train.getEndStationId() != null) {
            Station end = stationMapper.selectById(train.getEndStationId());
            if (end != null) {
                vo.setEndStationName(end.getStationName());
            }
        }

        // 售票开放时间
        if (train.getDepartDatetime() != null) {
            LocalDateTime openTime = calculateSaleOpenTime(train.getDepartDatetime());
            vo.setSaleOpenTime(openTime.format(FORMATTER));

            LocalDateTime now = LocalDateTime.now();
            vo.setDeparted(train.getDepartDatetime().isBefore(now));
            vo.setInSaleWindow(now.isAfter(openTime) && now.isBefore(train.getDepartDatetime()));
        }

        // 途经站点
        List<TrainStationRoute> routes = routeMapper.selectList(
                new LambdaQueryWrapper<TrainStationRoute>()
                        .eq(TrainStationRoute::getTrainId, train.getTrainId())
                        .orderByAsc(TrainStationRoute::getSort));
        List<TrainDTO.RouteItem> routeList = new ArrayList<>();
        for (TrainStationRoute route : routes) {
            TrainDTO.RouteItem item = new TrainDTO.RouteItem();
            item.setStationId(route.getStationId());
            item.setSort(route.getSort());
            item.setArriveDatetime(route.getArriveDatetime());
            item.setDepartDatetime(route.getDepartDatetime());
            routeList.add(item);
        }
        vo.setRouteList(routeList);

        return vo;
    }
}
