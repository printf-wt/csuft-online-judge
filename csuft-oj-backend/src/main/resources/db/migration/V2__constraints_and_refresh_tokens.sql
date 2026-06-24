CREATE TABLE tb_refresh_token (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME DEFAULT NULL,
    replaced_by_hash CHAR(64) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    KEY idx_refresh_user (user_id, expires_at),
    KEY idx_refresh_expiry (expires_at, revoked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE tb_problem
    ADD CONSTRAINT fk_problem_author FOREIGN KEY (author_id) REFERENCES tb_user(id) ON DELETE SET NULL;
ALTER TABLE tb_problem_tag
    ADD CONSTRAINT fk_problem_tag_problem FOREIGN KEY (problem_id) REFERENCES tb_problem(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_problem_tag_tag FOREIGN KEY (tag_id) REFERENCES tb_tag(id) ON DELETE CASCADE;
ALTER TABLE tb_test_case
    ADD CONSTRAINT fk_test_case_problem FOREIGN KEY (problem_id) REFERENCES tb_problem(id) ON DELETE CASCADE;
ALTER TABLE tb_contest
    ADD CONSTRAINT fk_contest_creator FOREIGN KEY (created_by) REFERENCES tb_user(id) ON DELETE SET NULL;
ALTER TABLE tb_contest_problem
    ADD CONSTRAINT fk_contest_problem_contest FOREIGN KEY (contest_id) REFERENCES tb_contest(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_contest_problem_problem FOREIGN KEY (problem_id) REFERENCES tb_problem(id) ON DELETE RESTRICT;
ALTER TABLE tb_contest_registration
    ADD CONSTRAINT fk_registration_contest FOREIGN KEY (contest_id) REFERENCES tb_contest(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_registration_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE;
ALTER TABLE tb_submission
    ADD CONSTRAINT fk_submission_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_submission_problem FOREIGN KEY (problem_id) REFERENCES tb_problem(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_submission_contest FOREIGN KEY (contest_id) REFERENCES tb_contest(id) ON DELETE SET NULL;
ALTER TABLE tb_notice
    ADD CONSTRAINT fk_notice_author FOREIGN KEY (author_id) REFERENCES tb_user(id) ON DELETE SET NULL;
ALTER TABLE tb_audit_log
    ADD CONSTRAINT fk_audit_operator FOREIGN KEY (operator_id) REFERENCES tb_user(id) ON DELETE SET NULL;
ALTER TABLE tb_refresh_token
    ADD CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE;

ALTER TABLE tb_user
    ADD CONSTRAINT chk_user_status CHECK (status IN (0, 1));
ALTER TABLE tb_problem
    ADD CONSTRAINT chk_problem_visible CHECK (is_visible IN (0, 1)),
    ADD CONSTRAINT chk_problem_limits CHECK (time_limit_ms > 0 AND memory_limit_kb > 0);
ALTER TABLE tb_contest
    ADD CONSTRAINT chk_contest_flags CHECK (is_public IN (0, 1) AND status IN (0, 1)),
    ADD CONSTRAINT chk_contest_time CHECK (end_time > start_time);
