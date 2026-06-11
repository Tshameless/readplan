package com.readplan.module.auth.service;

import com.readplan.common.exception.BusinessException;
import com.readplan.module.shared.payload.ReadPlanPayloads.AuthTokenResponse;
import com.readplan.module.shared.store.ReadPlanStore;
import com.readplan.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final ReadPlanStore readPlanStore;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(ReadPlanStore readPlanStore, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.readPlanStore = readPlanStore;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthTokenResponse register(String username, String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(400, "两次输入的密码不一致");
        }

        if (readPlanStore.findUserByUsername(username) != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        var user = readPlanStore.createUser(username, passwordEncoder.encode(password), "新阅读者", "USER");
        return new AuthTokenResponse(jwtService.generateToken(readPlanStore.toCurrentUser(user)));
    }

    public AuthTokenResponse login(String username, String password) {
        var user = readPlanStore.findUserByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.password())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        return new AuthTokenResponse(jwtService.generateToken(readPlanStore.toCurrentUser(user)));
    }
}
