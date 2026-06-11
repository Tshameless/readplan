package com.readplan.module.shared.store.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("import_candidate")
public class ImportCandidateEntity {

    @TableId
    private String olId;
    private String title;
    private String author;
    private Integer firstPublishYear;
    private String cover;
    private String isbn;
    private String description;
    private String tags;
}
