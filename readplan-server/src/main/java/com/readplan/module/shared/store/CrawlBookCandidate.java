package com.readplan.module.shared.store;

import java.util.List;

public record CrawlBookCandidate(
    String title,
    String author,
    String cover,
    Integer publishYear,
    String isbn,
    String olId,
    String description,
    List<String> tags
) {
}
