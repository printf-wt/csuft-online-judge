package com.csuft.oj.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NoticeUpdateRequest {

    @Size(min = 1, max = 255, message = "Notice title length must be between 1 and 255 characters")
    private String title;

    @Size(min = 1, max = 20000, message = "Notice content length must be between 1 and 20000 characters")
    private String content;

    @Min(value = 0, message = "Pinned flag must be 0 or 1")
    @Max(value = 1, message = "Pinned flag must be 0 or 1")
    private Integer isPinned;

    @Min(value = 0, message = "Visible flag must be 0 or 1")
    @Max(value = 1, message = "Visible flag must be 0 or 1")
    private Integer isVisible;
}
