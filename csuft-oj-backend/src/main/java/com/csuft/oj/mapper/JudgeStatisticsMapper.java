package com.csuft.oj.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface JudgeStatisticsMapper {

    @Insert("""
            INSERT IGNORE INTO tb_user_problem_solve
                (user_id, problem_id, first_accepted_submission_id, solved_at)
            VALUES
                (#{userId}, #{problemId}, #{submissionId}, #{solvedAt})
            """)
    int insertSolvedRecord(
            @Param("userId") Long userId,
            @Param("problemId") Long problemId,
            @Param("submissionId") Long submissionId,
            @Param("solvedAt") LocalDateTime solvedAt);

    @Update("""
            UPDATE tb_user
            SET global_ac_count = global_ac_count + 1
            WHERE id = #{userId}
            """)
    int incrementUserSolvedCount(@Param("userId") Long userId);

    @Update("""
            UPDATE tb_problem
            SET accepted_count = accepted_count + 1
            WHERE id = #{problemId}
            """)
    int incrementProblemSolvedCount(@Param("problemId") Long problemId);
}
