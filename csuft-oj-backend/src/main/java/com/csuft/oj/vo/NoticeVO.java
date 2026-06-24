package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoticeVO {
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private Integer isPinned;
    private Integer isVisible;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
