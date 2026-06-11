package com.readplan.module.user.controller;

import com.readplan.common.api.ApiResponse;
import com.readplan.common.security.CurrentUser;
import com.readplan.module.shared.payload.ReadPlanPayloads.UserInfoResponse;
import com.readplan.module.shared.store.ReadPlanStore;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final ReadPlanStore readPlanStore;

    public UserController(ReadPlanStore readPlanStore) {
        this.readPlanStore = readPlanStore;
    }

    @GetMapping("/info")
    public ApiResponse<UserInfoResponse> currentUser(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(readPlanStore.toUserInfo(currentUser));
    }
}
