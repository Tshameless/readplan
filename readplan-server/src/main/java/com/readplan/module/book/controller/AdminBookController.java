package com.readplan.module.book.controller;

import com.readplan.common.api.ApiResponse;
import com.readplan.module.shared.payload.ReadPlanPayloads.AdminImportCandidate;
import com.readplan.module.shared.payload.ReadPlanPayloads.BookSummary;
import com.readplan.module.shared.store.ReadPlanStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/books")
public class AdminBookController {

    private final ReadPlanStore readPlanStore;

    public AdminBookController(ReadPlanStore readPlanStore) {
        this.readPlanStore = readPlanStore;
    }

    @GetMapping
    public ApiResponse<List<BookSummary>> list() {
        return ApiResponse.success(readPlanStore.listAdminBooks());
    }

    @GetMapping("/import/candidates")
    public ApiResponse<List<AdminImportCandidate>> searchCandidates(@RequestParam(defaultValue = "") String keyword) {
        return ApiResponse.success(readPlanStore.searchImportCandidates(keyword));
    }

    @PostMapping("/import")
    public ApiResponse<List<BookSummary>> importFromOpenLibrary(@Valid @RequestBody ImportRequest request) {
        return ApiResponse.success(readPlanStore.importBooks(request.olIds()));
    }

    @PostMapping
    public ApiResponse<BookSummary> create(@Valid @RequestBody SaveBookRequest request) {
        return ApiResponse.success(readPlanStore.createBook(
            request.title(),
            request.author(),
            request.cover(),
            request.publishYear(),
            request.isbn(),
            request.olId(),
            request.description()
        ));
    }

    @PutMapping("/{id}")
    public ApiResponse<BookSummary> update(@PathVariable Long id, @Valid @RequestBody SaveBookRequest request) {
        return ApiResponse.success(readPlanStore.updateBook(
            id,
            request.title(),
            request.author(),
            request.cover(),
            request.publishYear(),
            request.isbn(),
            request.olId(),
            request.description()
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        readPlanStore.softDeleteBook(id);
        return ApiResponse.success(null);
    }

    public record ImportRequest(List<String> olIds) {
    }

    public record SaveBookRequest(
        @NotBlank(message = "书名不能为空") String title,
        String author,
        String cover,
        Integer publishYear,
        String isbn,
        String olId,
        String description
    ) {
    }
}
