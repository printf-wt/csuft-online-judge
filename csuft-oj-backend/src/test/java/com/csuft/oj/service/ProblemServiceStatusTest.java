package com.csuft.oj.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csuft.oj.entity.Problem;
import com.csuft.oj.entity.Submission;
import com.csuft.oj.mapper.ProblemMapper;
import com.csuft.oj.mapper.SubmissionMapper;
import com.csuft.oj.mapper.TestCaseMapper;
import com.csuft.oj.vo.PageResult;
import com.csuft.oj.vo.ProblemVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProblemServiceStatusTest {

    @Test
    void marksProblemsByCurrentUserSubmissionHistory() {
        ProblemMapper problemMapper = mock(ProblemMapper.class);
        SubmissionMapper submissionMapper = mock(SubmissionMapper.class);
        TestCaseMapper testCaseMapper = mock(TestCaseMapper.class);
        ProblemService service = new ProblemService(
                problemMapper,
                submissionMapper,
                testCaseMapper,
                "target/test-problem-status",
                1024 * 1024,
                10,
                1024 * 1024,
                1024 * 1024,
                100);

        Page<Problem> page = new Page<>(1, 10);
        page.setRecords(List.of(problem(1L), problem(2L), problem(3L)));
        page.setTotal(3);
        when(problemMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        when(submissionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                submission(1L, "WRONG_ANSWER"),
                submission(1L, "ACCEPTED"),
                submission(2L, "COMPILE_ERROR")));

        PageResult<ProblemVO> result = service.listProblems(1, 10, null, null, 99L, false);

        assertEquals("ACCEPTED", result.getRecords().get(0).getSubmissionStatus());
        assertEquals("WRONG_ANSWER", result.getRecords().get(1).getSubmissionStatus());
        assertEquals("NOT_SUBMITTED", result.getRecords().get(2).getSubmissionStatus());
    }

    private Problem problem(Long id) {
        Problem problem = new Problem();
        problem.setId(id);
        problem.setTitle("Problem " + id);
        problem.setDescription("Description");
        problem.setDifficulty("EASY");
        problem.setTimeLimitMs(1000);
        problem.setMemoryLimitKb(262144);
        problem.setIsVisible(1);
        problem.setAcceptedCount(0);
        problem.setSubmitCount(0);
        return problem;
    }

    private Submission submission(Long problemId, String status) {
        Submission submission = new Submission();
        submission.setProblemId(problemId);
        submission.setStatus(status);
        return submission;
    }
}
