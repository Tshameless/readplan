package com.readplan.module.shared.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("comment")
public class CommentEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long noteId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
