package com.csuft.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 题目-标签关联表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_problem_tag")
public class ProblemTag {

    /**
     * 关联ID，MyBatis-Plus 使用的自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题目ID，逻辑关联 tb_problem.id；与 tagId 构成复合业务主键。
     */
    private Long problemId;

    /**
     * 标签ID，逻辑关联 tb_tag.id；与 problemId 构成复合业务主键。
     */
    private Long tagId;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
