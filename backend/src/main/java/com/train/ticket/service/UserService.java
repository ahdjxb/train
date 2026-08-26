package com.train.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.train.ticket.common.Constants;
import com.train.ticket.dto.*;
import com.train.ticket.entity.TUser;
import com.train.ticket.mapper.TUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    private TUserMapper userMapper;

    /**
     * 简单加密：Base64编码
     */
    private String encrypt(String password) {
        try {
            return java.util.Base64.getEncoder().encodeToString(password.getBytes());
        } catch (Exception e) {
            return password;
        }
    }

    private String decrypt(String encrypted) {
        try {
            return new String(java.util.Base64.getDecoder().decode(encrypted));
        } catch (Exception e) {
            return encrypted;
        }
    }

    /**
     * 注册
     */
    public Map<String, Object> register(RegisterDTO dto) {
        // 检查账号是否已存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<TUser>()
                        .eq(TUser::getAccount, dto.getAccount()));
        if (count > 0) {
            throw new RuntimeException("账号已存在");
        }

        TUser user = new TUser();
        user.setUsername(dto.getUsername());
        user.setAccount(dto.getAccount());
        user.setPassword(encrypt(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setRole(Constants.ROLE_NORMAL);
        user.setIsLock(0);
        userMapper.insert(user);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getUserId());
        return result;
    }

    /**
     * 登录
     */
    public Map<String, Object> login(LoginDTO dto) {
        TUser user = userMapper.selectOne(
                new LambdaQueryWrapper<TUser>()
                        .eq(TUser::getAccount, dto.getAccount()));
        if (user == null) {
            throw new RuntimeException("账号不存在");
        }
        if (!encrypt(dto.getPassword()).equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        if (user.getIsLock() != null && user.getIsLock() == 1) {
            throw new RuntimeException("账号已被锁定，请联系管理员");
        }
        if (!Constants.ROLE_NORMAL.equals(user.getRole())) {
            throw new RuntimeException("请使用管理员登录入口");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getUserId());
        result.put("username", user.getUsername());
        result.put("account", user.getAccount());
        return result;
    }

    /**
     * 查询个人信息
     */
    public TUser getUserInfo(Long userId) {
        TUser user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    /**
     * 修改个人信息
     */
    public void updateUserInfo(Long userId, UpdateUserDTO dto) {
        TUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 如果修改账号，检查新账号是否重复
        if (dto.getAccount() != null && !dto.getAccount().equals(user.getAccount())) {
            Long count = userMapper.selectCount(
                    new LambdaQueryWrapper<TUser>()
                            .eq(TUser::getAccount, dto.getAccount())
                            .ne(TUser::getUserId, userId));
            if (count > 0) {
                throw new RuntimeException("账号已存在");
            }
            user.setAccount(dto.getAccount());
        }
        if (dto.getUsername() != null) {
            user.setUsername(dto.getUsername());
        }
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(encrypt(dto.getPassword()));
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        userMapper.updateById(user);
    }

    /**
     * 修改密码
     */
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        TUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!encrypt(dto.getOldPassword()).equals(user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        user.setPassword(encrypt(dto.getNewPassword()));
        userMapper.updateById(user);
    }

    /**
     * 账号注销
     */
    public void deleteUser(Long userId) {
        userMapper.deleteById(userId);
    }
}
