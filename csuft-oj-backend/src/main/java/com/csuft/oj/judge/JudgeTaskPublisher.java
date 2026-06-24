package com.csuft.oj.judge;

/**
 * Publishes a persisted submission for asynchronous judging.
 */
public interface JudgeTaskPublisher {

    boolean addSubmissionTask(Long submissionId);
}
