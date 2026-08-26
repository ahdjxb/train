package com.train.ticket.controller;

import com.train.ticket.common.Result;
import com.train.ticket.dto.TicketQueryDTO;
import com.train.ticket.entity.Station;
import com.train.ticket.service.TicketQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 车票查询Controller
 */
@RestController
@RequestMapping("/api/ticket")
public class TicketQueryController {

    @Autowired
    private TicketQueryService ticketQueryService;

    /**
     * 余票查询：直达优先，无直达才查中转
     */
    @PostMapping("/search")
    public Result<Map<String, Object>> searchTickets(@RequestBody TicketQueryDTO dto) {
        return Result.success(ticketQueryService.searchTickets(dto));
    }

    /**
     * 查询城市列表（去重）
     */
    @GetMapping("/cities")
    public Result<List<String>> listCities() {
        return Result.success(ticketQueryService.listCities());
    }

    /**
     * 查询某城市下的站点列表
     */
    @GetMapping("/stations-by-city")
    public Result<List<Station>> listStationsByCity(@RequestParam String city) {
        return Result.success(ticketQueryService.listStationsByCity(city));
    }
}
