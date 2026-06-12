package com.readplan.module.book.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookFileController {

    private final JdbcTemplate jdbcTemplate;

    public BookFileController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/files/books/db/{bookId}")
    public ResponseEntity<byte[]> getBookFileFromDb(@PathVariable Long bookId) {
        byte[] fileContent;
        try {
            fileContent = jdbcTemplate.queryForObject(
                "SELECT file_content FROM book_file WHERE book_id = ?",
                byte[].class,
                bookId
            );
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return ResponseEntity.notFound().build();
        }

        if (fileContent == null || fileContent.length == 0) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "book-" + bookId + ".pdf");
        // To allow inline viewing in browser:
        headers.set("Content-Disposition", "inline; filename=\"book-" + bookId + ".pdf\"");

        return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
    }
}
