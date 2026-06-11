package com.readplan.module.comment.controller;

import com.readplan.common.api.ApiResponse;
import com.readplan.common.security.CurrentUser;
import com.readplan.module.shared.payload.ReadPlanPayloads.CommentItem;
import com.readplan.module.shared.store.ReadPlanStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final ReadPlanStore readPlanStore;

    public CommentController(ReadPlanStore readPlanStore) {
        this.readPlanStore = readPlanStore;
    }

    @PostMapping("/comments")
    public ApiResponse<CommentItem> create(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody SaveCommentRequest request
    ) {
        return ApiResponse.success(readPlanStore.createComment(currentUser.id(), request.noteId(), request.content()));
    }

    @GetMapping("/notes/{noteId}/comments")
    public ApiResponse<List<CommentItem>> noteComments(
        @PathVariable Long noteId,
        @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ApiResponse.success(readPlanStore.getComments(noteId, currentUser == null ? null : currentUser.id()));
    }

    @DeleteMapping("/comments/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable Long id) {
        readPlanStore.deleteComment(currentUser.id(), id);
        return ApiResponse.success(null);
    }

    public record SaveCommentRequest(
        @NotNull(message = "noteId 不能为空") Long noteId,
        @NotBlank(message = "评论内容不能为空") String content
    ) {
    }
}
