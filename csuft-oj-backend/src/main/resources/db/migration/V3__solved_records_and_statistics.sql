CREATE TABLE tb_user_problem_solve (
    user_id BIGINT UNSIGNED NOT NULL,
    problem_id BIGINT UNSIGNED NOT NULL,
    first_accepted_submission_id BIGINT UNSIGNED DEFAULT NULL,
    solved_at DATETIME NOT NULL,
    PRIMARY KEY (user_id, problem_id),
    KEY idx_solve_problem (problem_id, solved_at, user_id),
    KEY idx_solve_submission (first_accepted_submission_id),
    CONSTRAINT fk_solve_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_solve_problem FOREIGN KEY (problem_id) REFERENCES tb_problem(id) ON DELETE CASCADE,
    CONSTRAINT fk_solve_submission FOREIGN KEY (first_accepted_submission_id) REFERENCES tb_submission(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tb_user_problem_solve (user_id, problem_id, first_accepted_submission_id, solved_at)
SELECT user_id, problem_id, MIN(id), MIN(COALESCE(judged_at, created_at))
FROM tb_submission
WHERE status = 'ACCEPTED'
GROUP BY user_id, problem_id;

UPDATE tb_user user_row
SET global_ac_count = (
    SELECT COUNT(*)
    FROM tb_user_problem_solve solved
    WHERE solved.user_id = user_row.id
);

UPDATE tb_problem problem_row
SET accepted_count = (
    SELECT COUNT(*)
    FROM tb_user_problem_solve solved
    WHERE solved.problem_id = problem_row.id
);

UPDATE tb_user user_row
SET submit_count = (
    SELECT COUNT(*)
    FROM tb_submission submission
    WHERE submission.user_id = user_row.id
);

UPDATE tb_problem problem_row
SET submit_count = (
    SELECT COUNT(*)
    FROM tb_submission submission
    WHERE submission.problem_id = problem_row.id
);
