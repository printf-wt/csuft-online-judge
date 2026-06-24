package com.csuft.oj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csuft.oj.entity.Submission;
import com.csuft.oj.vo.SubmissionActivityVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {

    @Select("""
            SELECT DATE(created_at) AS date, COUNT(*) AS count
            FROM tb_submission
            WHERE user_id = #{userId}
              AND created_at >= #{startDate}
            GROUP BY DATE(created_at)
            ORDER BY DATE(created_at)
            """)
    List<SubmissionActivityVO> selectDailyActivity(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate);
}
