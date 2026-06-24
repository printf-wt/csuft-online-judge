package com.csuft.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 公告表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_notice")
public class Notice {

    /**
     * 公告ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 公告标题。
     */
    private String title;

    /**
     * 公告内容。
     */
    private String content;

    /**
     * 作者用户ID，逻辑关联 tb_user.id。
     */
    private Long authorId;

    /**
     * 是否置顶：1置顶，0不置顶。
     */
    private Integer isPinned;

    /**
     * 是否可见：1可见，0隐藏。
     */
    private Integer isVisible;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
