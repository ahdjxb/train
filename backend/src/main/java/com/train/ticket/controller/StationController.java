package com.train.ticket.controller;

import com.train.ticket.common.Result;
import com.train.ticket.entity.Station;
import com.train.ticket.mapper.StationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 站点查询（供车次管理页面下拉选择使用）
 */
@RestController
@RequestMapping("/api/station")
public class StationController {

    @Autowired
    private StationMapper stationMapper;

    @GetMapping("/list")
    public Result<List<Station>> listStations() {
        return Result.success(stationMapper.selectList(null));
    }
}
