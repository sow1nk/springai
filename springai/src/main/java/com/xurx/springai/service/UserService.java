package com.xurx.springai.service;

import com.xurx.springai.entity.User;
import com.xurx.springai.mapper.UserMapper;
import com.xurx.springai.utils.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 根据userId查询用户，如果不存在则创建
     */
    public User getOrCreateUser(String userId) {
        User user = userMapper.selectByUserId(userId);
        if (user == null) {
            user = new User();
            user.setUserId(userId);
            userMapper.insert(user);
            log.info("创建新用户: {}", userId);
        }
        return user;
    }

    /**
     * 根据userId查询用户
     */
    public User getUserByUserId(String userId) {
        return userMapper.selectByUserId(userId);
    }

    /**
     * 根据username查询用户
     */
    public User getUserByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    /**
     * 更新用户信息
     */
    public void updateUser(User user) {
        userMapper.update(user);
    }

    /**
     * 注册新用户
     *
     * @param username    用户名
     * @param rawPassword 原始密码
     * @param email       邮箱
     * @return 新用户
     */
    public User registerUser(String username, String rawPassword, String email, String userId) {
        // 检查用户名是否已存在
        User existingUser = userMapper.selectByUsername(username);
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建新用户
        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmail(email);

        userMapper.insert(user);
        log.info("用户注册成功: username={}, userId={}", username, userId);

        return user;
    }

    /**
     * 验证用户登录
     *
     * @param username    用户名
     * @param rawPassword 原始密码
     * @return 用户信息，如果验证失败返回null
     */
    public User validateLogin(String username, String rawPassword) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            log.warn("用户不存在: {}", username);
            return null;
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            log.warn("密码错误: {}", username);
            return null;
        }

        log.info("用户登录成功: username={}, userId={}", username, user.getUserId());
        return user;
    }
}
