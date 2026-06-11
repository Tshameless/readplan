package com.readplan.module.auth.controller;

import com.readplan.common.api.ApiResponse;
import com.readplan.module.auth.service.AuthService;
import com.readplan.module.shared.payload.ReadPlanPayloads.AuthTokenResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthTokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request.username(), request.password(), request.confirmPassword()));
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request.username(), request.password()));
    }

    public record RegisterRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password,
        @NotBlank(message = "确认密码不能为空") String confirmPassword
    ) {
    }

    public record LoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password
    ) {
    }

}
