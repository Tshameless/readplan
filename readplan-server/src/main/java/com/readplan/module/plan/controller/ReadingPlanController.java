package com.readplan.module.plan.controller;

import com.readplan.common.api.ApiResponse;
import com.readplan.common.security.CurrentUser;
import com.readplan.module.shared.payload.ReadPlanPayloads.ReadingPlanItem;
import com.readplan.module.shared.store.ReadPlanStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
public class ReadingPlanController {

    private final ReadPlanStore readPlanStore;

    public ReadingPlanController(ReadPlanStore readPlanStore) {
        this.readPlanStore = readPlanStore;
    }

    @PostMapping
    public ApiResponse<ReadingPlanItem> create(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody SavePlanRequest request
    ) {
        return ApiResponse.success(readPlanStore.addPlan(currentUser.id(), request.bookId(), request.status()));
    }

    @GetMapping("/me")
    public ApiResponse<List<ReadingPlanItem>> currentUserPlans(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(readPlanStore.getPlans(currentUser.id()));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<ReadingPlanItem> updateStatus(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable Long id,
        @Valid @RequestBody UpdateStatusRequest request
    ) {
        return ApiResponse.success(readPlanStore.updatePlanStatus(currentUser.id(), id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable Long id) {
        readPlanStore.deletePlan(currentUser.id(), id);
        return ApiResponse.success(null);
    }

    public record SavePlanRequest(@NotNull(message = "bookId 不能为空") Long bookId, Integer status) {
    }

    public record UpdateStatusRequest(@NotNull(message = "status 不能为空") Integer status) {
    }
}
