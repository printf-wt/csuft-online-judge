package com.csuft.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 测试用例表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_test_case")
public class TestCase {

    /**
     * 测试用例ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题目ID，逻辑关联 tb_problem.id。
     */
    private Long problemId;

    /**
     * 输入文件路径。
     */
    private String inputPath;

    /**
     * 输出文件路径。
     */
    private String outputPath;

    /**
     * 输入文件前缀预览，应用层截断为2KB。
     */
    private String inputPreview;

    /**
     * 输出文件前缀预览，应用层截断为2KB。
     */
    private String outputPreview;

    /**
     * 测试点分值，IOI赛制可用。
     */
    private Integer score;

    /**
     * 测试点排序。
     */
    private Integer sortOrder;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
