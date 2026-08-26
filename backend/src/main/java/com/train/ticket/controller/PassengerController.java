package com.train.ticket.controller;

import com.train.ticket.common.Result;
import com.train.ticket.dto.PassengerDTO;
import com.train.ticket.entity.Passenger;
import com.train.ticket.service.PassengerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passenger")
public class PassengerController {

    @Autowired
    private PassengerService passengerService;

    @PostMapping("/{userId}")
    public Result<?> addPassenger(@PathVariable Long userId, @Valid @RequestBody PassengerDTO dto) {
        return Result.success(passengerService.addPassenger(userId, dto));
    }

    @GetMapping("/list/{userId}")
    public Result<List<Passenger>> listPassengers(@PathVariable Long userId) {
        return Result.success(passengerService.listPassengers(userId));
    }

    @DeleteMapping("/{userId}/{passengerId}")
    public Result<?> deletePassenger(@PathVariable Long userId, @PathVariable Long passengerId) {
        passengerService.deletePassenger(userId, passengerId);
        return Result.success();
    }
}
