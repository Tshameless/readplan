package com.readplan.module.note.controller;

import com.readplan.common.api.ApiResponse;
import com.readplan.common.security.CurrentUser;
import com.readplan.module.shared.payload.ReadPlanPayloads.PublicNote;
import com.readplan.module.shared.payload.ReadPlanPayloads.UserNoteSummary;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class NoteController {

    private final ReadPlanStore readPlanStore;

    public NoteController(ReadPlanStore readPlanStore) {
        this.readPlanStore = readPlanStore;
    }

    @PostMapping("/notes")
    public ApiResponse<UserNoteSummary> create(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody SaveNoteRequest request
    ) {
        return ApiResponse.success(readPlanStore.createNote(
            currentUser.id(),
            request.bookId(),
            request.title(),
            request.content()
        ));
    }

    @GetMapping("/notes/me")
    public ApiResponse<List<UserNoteSummary>> currentUserNotes(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(readPlanStore.getUserNotes(currentUser.id()));
    }

    @GetMapping("/books/{bookId}/notes")
    public ApiResponse<List<PublicNote>> bookNotes(@PathVariable Long bookId) {
        return ApiResponse.success(readPlanStore.getBookNotes(bookId));
    }

    @PutMapping("/notes/{id}")
    public ApiResponse<UserNoteSummary> update(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable Long id,
        @Valid @RequestBody SaveNoteRequest request
    ) {
        return ApiResponse.success(readPlanStore.updateNote(currentUser.id(), id, request.title(), request.content()));
    }

    @DeleteMapping("/notes/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable Long id) {
        readPlanStore.deleteNote(currentUser.id(), id);
        return ApiResponse.success(null);
    }

    public record SaveNoteRequest(
        @NotNull(message = "bookId 不能为空") Long bookId,
        @NotBlank(message = "标题不能为空") String title,
        @NotBlank(message = "内容不能为空") String content
    ) {
    }
}
