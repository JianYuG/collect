package com.example.auth.controller;

import com.example.auth.entity.User;
import com.example.auth.exception.Result;
import com.example.auth.service.UserService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Void> register(@Validated @RequestBody RegisterRequest request) {
        log.info("注册请求: username={}", request.getUsername());
        userService.register(
                request.getUsername(),
                request.getPassword(),
                request.getNickname(),
                request.getEmail(),
                request.getPhone()
        );
        return Result.success("注册成功", null);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        log.info("登录请求: username={}", request.getUsername());
        String token = userService.login(request.getUsername(), request.getPassword());
        return Result.success("登录成功", new LoginResponse(token));
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/user-info")
    public Result<User> getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new com.example.auth.exception.BusinessException(401, "未登录或Token无效");
        }
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        return Result.success(user);
    }

    private String extractUsernameFromToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new com.example.auth.exception.BusinessException(401, "未登录或Token无效");
        }
        // 实际由 JwtAuthenticationFilter 处理，此处仅做兜底
        return null;
    }

    // ==================== 请求/响应 DTO ====================

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度为3-50个字符")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 64, max = 200, message = "密码格式不正确")
        private String password;

        @Size(max = 50, message = "昵称最多50个字符")
        private String nickname;

        @Size(max = 100, message = "邮箱最多100个字符")
        private String email;

        @Size(max = 20, message = "手机号最多20个字符")
        private String phone;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class LoginResponse {
        private String token;

        public LoginResponse(String token) {
            this.token = token;
        }
    }
}
