package com.train.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.train.ticket.common.Constants;
import com.train.ticket.dto.ChangePasswordDTO;
import com.train.ticket.dto.LoginDTO;
import com.train.ticket.entity.TUser;
import com.train.ticket.mapper.TUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdminService {

    @Autowired
    private TUserMapper userMapper;

    private String encrypt(String password) {
        try {
            return java.util.Base64.getEncoder().encodeToString(password.getBytes());
        } catch (Exception e) {
            return password;
        }
    }

    /**
     * 管理员登录
     */
    public Map<String, Object> login(LoginDTO dto) {
        TUser user = userMapper.selectOne(
                new LambdaQueryWrapper<TUser>()
                        .eq(TUser::getAccount, dto.getAccount()));
        if (user == null) {
            throw new RuntimeException("管理员账号不存在");
        }
        if (!Constants.ROLE_ADMIN.equals(user.getRole())) {
            throw new RuntimeException("非管理员账号");
        }
        if (!encrypt(dto.getPassword()).equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getUserId());
        result.put("username", user.getUsername());
        result.put("account", user.getAccount());
        return result;
    }

    /**
     * 修改管理员密码
     */
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        TUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("管理员不存在");
        }
        if (!encrypt(dto.getOldPassword()).equals(user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        user.setPassword(encrypt(dto.getNewPassword()));
        userMapper.updateById(user);
    }
}
