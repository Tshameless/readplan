package com.readplan.common.api;

import java.util.List;

public record PageResult<T>(long total, long pages, List<T> records) {

    public static <T> PageResult<T> of(long total, long pages, List<T> records) {
        return new PageResult<>(total, pages, records);
    }
}
