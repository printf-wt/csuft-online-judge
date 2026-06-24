package com.csuft.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 算法标签表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_tag")
public class Tag {

    /**
     * 标签ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 标签名称。
     */
    private String name;

    /**
     * 标签颜色。
     */
    private String color;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
