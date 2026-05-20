package com.example.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.auth.entity.User;
import com.example.auth.exception.BusinessException;
import com.example.auth.mapper.UserMapper;
import com.example.auth.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户注册
     */
    public void register(String username, String password, String nickname, String email, String phone) {
        // 1. 校验用户名是否已存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        Long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(400, "用户名已存在");
        }

        // 2. 校验邮箱是否已存在
        if (email != null && !email.trim().isEmpty()) {
            LambdaQueryWrapper<User> emailQuery = new LambdaQueryWrapper<>();
            emailQuery.eq(User::getEmail, email);
            Long emailCount = userMapper.selectCount(emailQuery);
            if (emailCount > 0) {
                throw new BusinessException(400, "邮箱已被注册");
            }
        }

        // 3. 校验手机号是否已存在
        if (phone != null && !phone.trim().isEmpty()) {
            LambdaQueryWrapper<User> phoneQuery = new LambdaQueryWrapper<>();
            phoneQuery.eq(User::getPhone, phone);
            Long phoneCount = userMapper.selectCount(phoneQuery);
            if (phoneCount > 0) {
                throw new BusinessException(400, "手机号已被注册");
            }
        }

        // 4. 创建用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null && !nickname.trim().isEmpty() ? nickname : username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        try {
            int rows = userMapper.insert(user);
            if (rows <= 0) {
                throw new BusinessException(500, "注册失败，请稍后重试");
            }
            log.info("用户注册成功: username={}", username);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("用户注册异常: username={}, error={}", username, e.getMessage(), e);
            throw new BusinessException(500, "注册失败，请稍后重试");
        }
    }

    /**
     * 用户登录，返回 JWT Token
     */
    public String login(String username, String password) {
        // 1. 查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            log.warn("登录失败 - 用户不存在: username={}", username);
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 2. 校验密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("登录失败 - 密码错误: username={}", username);
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 3. 校验账号状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            log.warn("登录失败 - 账号已禁用: username={}", username);
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        // 4. 生成 Token
        try {
            String token = jwtUtil.generateToken(username);
            log.info("用户登录成功: username={}", username);
            return token;
        } catch (Exception e) {
            log.error("生成Token异常: username={}, error={}", username, e.getMessage(), e);
            throw new BusinessException(500, "登录失败，请稍后重试");
        }
    }

    /**
     * 根据 username 查询用户信息
     */
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 清除密码，不返回给前端
        user.setPassword(null);
        return user;
    }
}
