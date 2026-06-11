package com.readplan.module.shared.payload;

import java.util.List;

public final class ReadPlanPayloads {

    private ReadPlanPayloads() {
    }

    public record AuthTokenResponse(String token) {
    }

    public record UserInfoResponse(
        Long id,
        String username,
        String nickname,
        List<String> roles,
        List<String> permissions
    ) {
    }

    public record BookSummary(
        Long id,
        String title,
        String author,
        String cover,
        Integer publishYear,
        String description,
        List<String> tags,
        Boolean imported,
        String isbn,
        String olId
    ) {
    }

    public record PublicNote(
        Long id,
        String title,
        String authorName,
        String contentPreview,
        String createdAt,
        Integer commentCount
    ) {
    }

    public record BookDetail(
        Long id,
        String title,
        String author,
        String cover,
        Integer publishYear,
        String description,
        List<String> tags,
        Boolean imported,
        String isbn,
        String openLibraryId,
        Integer noteCount,
        Integer planCount,
        List<PublicNote> notes
    ) {
    }

    public record ReadingPlanItem(
        Long id,
        BookSummary book,
        Integer status,
        String shelfState,
        String updatedAt,
        Boolean allowNote
    ) {
    }

    public record UserNoteSummary(
        Long id,
        Long bookId,
        String bookTitle,
        String title,
        String excerpt,
        String createdAt,
        Integer commentCount
    ) {
    }

    public record CommentItem(
        Long id,
        String username,
        String content,
        String createdAt,
        Boolean canDelete
    ) {
    }

    public record AdminImportCandidate(
        String olId,
        String title,
        String author,
        Integer firstPublishYear,
        String cover,
        Boolean selected
    ) {
    }
}
