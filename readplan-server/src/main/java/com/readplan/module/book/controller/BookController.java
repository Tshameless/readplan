package com.readplan.module.book.controller;

import com.readplan.common.api.ApiResponse;
import com.readplan.common.api.PageResult;
import com.readplan.module.shared.payload.ReadPlanPayloads.BookDetail;
import com.readplan.module.shared.payload.ReadPlanPayloads.BookSummary;
import com.readplan.module.shared.store.ReadPlanStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final ReadPlanStore readPlanStore;

    public BookController(ReadPlanStore readPlanStore) {
        this.readPlanStore = readPlanStore;
    }

    @GetMapping
    public ApiResponse<PageResult<BookSummary>> list(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return ApiResponse.success(readPlanStore.listBooks(keyword, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<BookDetail> detail(@PathVariable Long id) {
        return ApiResponse.success(readPlanStore.getBookDetail(id));
    }
}
