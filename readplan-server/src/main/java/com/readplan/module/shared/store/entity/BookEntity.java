package com.readplan.module.shared.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("book")
public class BookEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String author;
    private String cover;
    private Integer publishYear;
    private String isbn;
    private String olId;
    private String description;
    private String tags;
    private String filePath;
    private String fileType;
    private Integer imported;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
