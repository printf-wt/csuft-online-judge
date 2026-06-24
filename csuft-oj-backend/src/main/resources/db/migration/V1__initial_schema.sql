CREATE TABLE tb_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) DEFAULT NULL,
    email VARCHAR(128) DEFAULT NULL,
    role ENUM('STUDENT', 'TEACHER', 'ADMIN') NOT NULL DEFAULT 'STUDENT',
    global_ac_count INT UNSIGNED NOT NULL DEFAULT 0,
    submit_count INT UNSIGNED NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    KEY idx_solved_submit (global_ac_count DESC, submit_count ASC, id ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tb_problem (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description LONGTEXT NOT NULL,
    input_description TEXT DEFAULT NULL,
    output_description TEXT DEFAULT NULL,
    sample_input TEXT DEFAULT NULL,
    sample_output TEXT DEFAULT NULL,
    difficulty ENUM('EASY', 'MEDIUM', 'HARD') NOT NULL DEFAULT 'EASY',
    time_limit_ms INT UNSIGNED NOT NULL DEFAULT 1000,
    memory_limit_kb INT UNSIGNED NOT NULL DEFAULT 262144,
    author_id BIGINT UNSIGNED DEFAULT NULL,
    is_visible TINYINT NOT NULL DEFAULT 1,
    accepted_count INT UNSIGNED NOT NULL DEFAULT 0,
    submit_count INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_visible (is_visible, id),
    KEY idx_author_id (author_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tb_tag (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    color VARCHAR(32) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tb_problem_tag (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    problem_id BIGINT UNSIGNED NOT NULL,
    tag_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_problem_tag (problem_id, tag_id),
    KEY idx_tag_problem (tag_id, problem_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tb_test_case (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    problem_id BIGINT UNSIGNED NOT NULL,
    input_path VARCHAR(512) NOT NULL,
    output_path VARCHAR(512) NOT NULL,
    input_preview TEXT DEFAULT NULL,
    output_preview TEXT DEFAULT NULL,
    score INT UNSIGNED NOT NULL DEFAULT 0,
    sort_order INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_problem_order (problem_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tb_contest (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description TEXT DEFAULT NULL,
    rule_type ENUM('ACM', 'IOI') NOT NULL DEFAULT 'ACM',
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    is_public TINYINT NOT NULL DEFAULT 1,
    status TINYINT NOT NULL DEFAULT 1,
    created_by BIGINT UNSIGNED DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_start_end (start_time, end_time),
    KEY idx_created_by (created_by, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tb_contest_problem (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    contest_id BIGINT UNSIGNED NOT NULL,
    problem_id BIGINT UNSIGNED NOT NULL,
    alias VARCHAR(16) NOT NULL,
    sort_order INT UNSIGNED NOT NULL DEFAULT 0,
    score INT UNSIGNED NOT NULL DEFAULT 100,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_contest_problem (contest_id, problem_id),
    UNIQUE KEY uk_contest_alias (contest_id, alias),
    KEY idx_problem_id (problem_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tb_contest_registration (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    contest_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    status ENUM('REGISTERED', 'CANCELLED') NOT NULL DEFAULT 'REGISTERED',
    registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_contest_user (contest_id, user_id),
    KEY idx_user_contest (user_id, contest_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tb_submission (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    problem_id BIGINT UNSIGNED NOT NULL,
    contest_id BIGINT UNSIGNED DEFAULT NULL,
    language VARCHAR(32) NOT NULL,
    code LONGTEXT NOT NULL,
    code_length INT UNSIGNED NOT NULL DEFAULT 0,
    status ENUM('PENDING','JUDGING','COMPILING','RUNNING','ACCEPTED','WRONG_ANSWER',
        'TIME_LIMIT_EXCEEDED','MEMORY_LIMIT_EXCEEDED','RUNTIME_ERROR','COMPILE_ERROR','SYSTEM_ERROR')
        NOT NULL DEFAULT 'PENDING',
    score INT UNSIGNED NOT NULL DEFAULT 0,
    time_used_ms INT UNSIGNED DEFAULT NULL,
    memory_used_kb INT UNSIGNED DEFAULT NULL,
    judge_message TEXT DEFAULT NULL,
    error_log TEXT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    judged_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_status (status, created_at, id),
    KEY idx_contest_user (contest_id, user_id, problem_id, created_at),
    KEY idx_user_problem (user_id, problem_id, created_at),
    KEY idx_problem_status (problem_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tb_notice (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL,
    author_id BIGINT UNSIGNED DEFAULT NULL,
    is_pinned TINYINT NOT NULL DEFAULT 0,
    is_visible TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_pinned_created (is_visible, is_pinned DESC, created_at DESC, id DESC),
    KEY idx_author_id (author_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tb_audit_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    operator_id BIGINT UNSIGNED DEFAULT NULL,
    action VARCHAR(128) NOT NULL,
    target_type VARCHAR(64) DEFAULT NULL,
    target_id BIGINT UNSIGNED DEFAULT NULL,
    ip_address VARCHAR(64) DEFAULT NULL,
    user_agent VARCHAR(512) DEFAULT NULL,
    detail JSON DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_operator_created (operator_id, created_at DESC),
    KEY idx_target (target_type, target_id),
    KEY idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
