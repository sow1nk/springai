package com.xurx.springai.controller;

import com.xurx.springai.dto.LoginRequest;
import com.xurx.springai.dto.RegisterRequest;
import com.xurx.springai.entity.User;
import com.xurx.springai.service.UserService;
import com.xurx.springai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        log.info("username: {}  password: {}", username, password);

        if (username == null || username.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名不能为空"));
        }

        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "密码不能为空"));
        }

        // 验证用户登录
        User user = userService.validateLogin(username, password);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名或密码错误"));
        }

        // 生成JWT令牌
        String token = jwtUtils.generateToken(user.getUserId());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userId", user.getUserId());
        response.put("username", user.getUsername());

        log.info("用户登录成功: username={}, userId={}", username, user.getUserId());

        return ResponseEntity.ok(response);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest registerRequest) {
        String username = registerRequest.getUsername();
        String password = registerRequest.getPassword();
        String email = registerRequest.getEmail();
        log.info("用户注册信息：username: {} password: {} email: {}", username, password, email);

        if (username == null || username.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名不能为空"));
        }

        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "密码不能为空"));
        }

        if (password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "密码长度至少为6个字符"));
        }

        // 生成userId
        String userId = UUID.randomUUID().toString();

        try {
            // 注册新用户
            User user = userService.registerUser(username, password, email, userId);

            // 生成JWT令牌
            String token = jwtUtils.generateToken(user.getUserId());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("userId", user.getUserId());
            response.put("username", user.getUsername());

            log.info("用户注册成功: username={}, userId={}", username, userId);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("用户注册失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 验证令牌
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "无效的令牌格式"));
        }

        String token = authHeader.substring(7);
        boolean isValid = jwtUtils.validateToken(token);

        if (isValid) {
            String userId = jwtUtils.getUserIdFromToken(token);
            return ResponseEntity.ok(Map.of("valid", true, "userId", userId));
        } else {
            return ResponseEntity.ok(Map.of("valid", false));
        }
    }
}
