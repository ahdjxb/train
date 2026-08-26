package com.train.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.train.ticket.dto.PassengerDTO;
import com.train.ticket.entity.Passenger;
import com.train.ticket.mapper.PassengerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PassengerService {

    @Autowired
    private PassengerMapper passengerMapper;

    /**
     * 新增乘车人
     */
    public Passenger addPassenger(Long userId, PassengerDTO dto) {
        Passenger passenger = new Passenger();
        passenger.setUserId(userId);
        passenger.setRealName(dto.getRealName());
        passenger.setIdCard(dto.getIdCard());
        passengerMapper.insert(passenger);
        return passenger;
    }

    /**
     * 查询当前用户的全部乘车人
     */
    public List<Passenger> listPassengers(Long userId) {
        return passengerMapper.selectList(
                new LambdaQueryWrapper<Passenger>()
                        .eq(Passenger::getUserId, userId)
                        .orderByDesc(Passenger::getPassengerId));
    }

    /**
     * 删除乘车人
     */
    public void deletePassenger(Long userId, Long passengerId) {
        Passenger passenger = passengerMapper.selectById(passengerId);
        if (passenger == null) {
            throw new RuntimeException("乘车人不存在");
        }
        if (!passenger.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除他人乘车人");
        }
        passengerMapper.deleteById(passengerId);
    }
}
