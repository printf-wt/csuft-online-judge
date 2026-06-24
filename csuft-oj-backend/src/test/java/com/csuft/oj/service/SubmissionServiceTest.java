package com.csuft.oj.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csuft.oj.dto.SubmissionCreateRequest;
import com.csuft.oj.entity.Contest;
import com.csuft.oj.entity.Problem;
import com.csuft.oj.entity.Submission;
import com.csuft.oj.exception.BusinessException;
import com.csuft.oj.judge.JudgeTaskPublisher;
import com.csuft.oj.mapper.ContestMapper;
import com.csuft.oj.mapper.ContestProblemMapper;
import com.csuft.oj.mapper.ContestRegistrationMapper;
import com.csuft.oj.mapper.ProblemMapper;
import com.csuft.oj.mapper.SubmissionMapper;
import com.csuft.oj.mapper.UserMapper;
import com.csuft.oj.vo.PageResult;
import com.csuft.oj.vo.SubmissionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionMapper submissionMapper;
    @Mock
    private ProblemMapper problemMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ContestMapper contestMapper;
    @Mock
    private ContestProblemMapper contestProblemMapper;
    @Mock
    private ContestRegistrationMapper contestRegistrationMapper;
    @Mock
    private JudgeTaskPublisher judgeTaskPublisher;

    private SubmissionService service;

    @BeforeEach
    void setUp() {
        service = new SubmissionService(
                submissionMapper,
                problemMapper,
                userMapper,
                contestMapper,
                contestProblemMapper,
                contestRegistrationMapper,
                judgeTaskPublisher,
                1024);
    }

    @Test
    void contestSubmissionRequiresRegistration() {
        when(problemMapper.selectById(10L)).thenReturn(problem(10L, 0));
        when(contestMapper.selectById(20L)).thenReturn(activeContest(20L));
        when(contestProblemMapper.selectCount(any())).thenReturn(1L);
        when(contestRegistrationMapper.selectCount(any())).thenReturn(0L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.submit(request(10L, 20L), 30L));

        assertEquals(403, exception.getCode());
        assertEquals("Contest registration is required before submitting", exception.getMessage());
    }

    @Test
    void validContestSubmissionIsPersistedAndDispatched() {
        when(problemMapper.selectById(10L)).thenReturn(problem(10L, 0));
        when(contestMapper.selectById(20L)).thenReturn(activeContest(20L));
        when(contestProblemMapper.selectCount(any())).thenReturn(1L);
        when(contestRegistrationMapper.selectCount(any())).thenReturn(1L);
        doAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            submission.setId(99L);
            return 1;
        }).when(submissionMapper).insert(any(Submission.class));

        service.submit(request(10L, 20L), 30L);

        verify(submissionMapper).insert(any(Submission.class));
        verify(userMapper).update(any(), any());
        verify(problemMapper).update(any(), any());
        verify(judgeTaskPublisher).addSubmissionTask(99L);
    }

    @Test
    void submissionListDoesNotExposeSourceCode() {
        Submission submission = new Submission();
        submission.setId(99L);
        submission.setUserId(30L);
        submission.setProblemId(10L);
        submission.setLanguage("C++");
        submission.setCode("secret source code");
        submission.setCodeLength(18);
        submission.setStatus("ACCEPTED");
        Page<Submission> page = new Page<>(1, 10);
        page.setRecords(List.of(submission));
        page.setTotal(1);
        when(submissionMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResult<SubmissionVO> result = service.listSubmissions(
                1, 10, null, null, null, null, null, 30L, false);

        assertEquals(1, result.getRecords().size());
        assertNull(result.getRecords().get(0).getCode());
    }

    private SubmissionCreateRequest request(Long problemId, Long contestId) {
        return new SubmissionCreateRequest(problemId, "int main() { return 0; }", "C++", contestId);
    }

    private Problem problem(Long id, int visible) {
        Problem problem = new Problem();
        problem.setId(id);
        problem.setIsVisible(visible);
        return problem;
    }

    private Contest activeContest(Long id) {
        Contest contest = new Contest();
        contest.setId(id);
        contest.setStatus(1);
        contest.setStartTime(LocalDateTime.now().minusHours(1));
        contest.setEndTime(LocalDateTime.now().plusHours(1));
        return contest;
    }
}
