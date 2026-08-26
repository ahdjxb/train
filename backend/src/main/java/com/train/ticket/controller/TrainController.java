package com.train.ticket.controller;

import com.train.ticket.common.Result;
import com.train.ticket.dto.TrainDTO;
import com.train.ticket.dto.TrainVO;
import com.train.ticket.service.TrainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/train")
public class TrainController {

    @Autowired
    private TrainService trainService;

    @PostMapping
    public Result<?> addTrain(@RequestBody TrainDTO dto) {
        return Result.success(trainService.addTrain(dto));
    }

    @PutMapping
    public Result<?> updateTrain(@RequestBody TrainDTO dto) {
        trainService.updateTrain(dto);
        return Result.success();
    }

    @GetMapping("/{trainId}")
    public Result<TrainVO> getTrain(@PathVariable Long trainId) {
        return Result.success(trainService.getTrain(trainId));
    }

    @GetMapping("/list")
    public Result<List<TrainVO>> listTrains(
            @RequestParam(required = false) String trainNo,
            @RequestParam(required = false) String trainType) {
        return Result.success(trainService.listTrains(trainNo, trainType));
    }

    /**
     * 改签查询：同始发/终点城市、今日之后、正在售票的车次
     */
    @GetMapping("/list-for-change")
    public Result<List<TrainVO>> listTrainsForChange(
            @RequestParam String startCity,
            @RequestParam String endCity,
            @RequestParam(required = false) String trainType) {
        return Result.success(trainService.listTrainsForChange(startCity, endCity, trainType));
    }

    @DeleteMapping("/{trainId}")
    public Result<?> deleteTrain(@PathVariable Long trainId) {
        trainService.deleteTrain(trainId);
        return Result.success();
    }

    @PutMapping("/sale-status/{trainId}")
    public Result<?> setSaleStatus(
            @PathVariable Long trainId,
            @RequestParam Integer saleStatus) {
        trainService.setSaleStatus(trainId, saleStatus);
        return Result.success();
    }
}
