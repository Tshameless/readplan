package com.readplan.module.dashboard.controller;

import com.readplan.common.api.ApiResponse;
import com.readplan.module.shared.payload.ReadPlanPayloads.DashboardStat;
import com.readplan.module.shared.store.ReadPlanStore;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ReadPlanStore readPlanStore;

    public DashboardController(ReadPlanStore readPlanStore) {
        this.readPlanStore = readPlanStore;
    }

    @GetMapping("/stats")
    public ApiResponse<List<DashboardStat>> stats() {
        return ApiResponse.success(readPlanStore.getDashboardStats());
    }
}
