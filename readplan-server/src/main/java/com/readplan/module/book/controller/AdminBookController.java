package com.readplan.module.book.controller;

import com.readplan.common.api.ApiResponse;
import com.readplan.module.shared.payload.ReadPlanPayloads.AdminImportCandidate;
import com.readplan.module.shared.payload.ReadPlanPayloads.BookSummary;
import com.readplan.module.shared.payload.ReadPlanPayloads.LegalBookResourceCandidate;
import com.readplan.module.shared.store.ReadPlanStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/crawl")
    public ApiResponse<List<AdminImportCandidate>> crawlCandidates(@RequestParam(defaultValue = "") String keyword) {
        return ApiResponse.success(readPlanStore.crawlImportCandidates(keyword));
    }

    @GetMapping("/legal-resources")
    public ApiResponse<List<LegalBookResourceCandidate>> searchLegalResources(
        @RequestParam(defaultValue = "") String keyword
    ) {
        return ApiResponse.success(readPlanStore.searchLegalResourceCandidates(keyword));
    }

    @PostMapping("/import")
    public ApiResponse<List<BookSummary>> importFromOpenLibrary(@Valid @RequestBody ImportRequest request) {
        return ApiResponse.success(readPlanStore.importBooks(request.olIds()));
    }

    @PostMapping("/legal-resources/import")
    public ApiResponse<List<BookSummary>> importFromLegalResources(@Valid @RequestBody ImportLegalResourcesRequest request) {
        List<LegalBookResourceCandidate> selected = request.resources() == null
            ? List.of()
            : request.resources().stream().filter(resource -> Boolean.TRUE.equals(resource.selected())).toList();
        return ApiResponse.success(readPlanStore.importBooksFromLegalResources(selected, request.tags()));
    }

    @PostMapping("/upload")
    public ApiResponse<List<BookSummary>> uploadBooks(
        @RequestPart("file") MultipartFile file,
        @RequestParam(value = "tags", required = false) String tags
    ) {
        return ApiResponse.success(readPlanStore.importBooksFromFile(file, tags));
    }

    @PostMapping("/{id}/file")
    public ApiResponse<BookSummary> uploadFile(
        @PathVariable Long id,
        @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.success(readPlanStore.uploadBookFile(id, file));
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
            request.description(),
            request.tags()
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
            request.description(),
            request.tags()
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        readPlanStore.softDeleteBook(id);
        return ApiResponse.success(null);
    }

    public record ImportRequest(List<String> olIds) {
    }

    public record ImportLegalResourcesRequest(
        List<LegalBookResourceCandidate> resources,
        List<String> tags
    ) {
    }

    public record SaveBookRequest(
        @NotBlank(message = "书名不能为空") String title,
        String author,
        String cover,
        Integer publishYear,
        String isbn,
        String olId,
        String description,
        List<String> tags
    ) {
    }
}
