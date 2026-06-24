SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS tb_audit_log;
DROP TABLE IF EXISTS tb_notice;
DROP TABLE IF EXISTS tb_submission;
DROP TABLE IF EXISTS tb_contest_registration;
DROP TABLE IF EXISTS tb_contest_problem;
DROP TABLE IF EXISTS tb_contest;
DROP TABLE IF EXISTS tb_test_case;
DROP TABLE IF EXISTS tb_problem_tag;
DROP TABLE IF EXISTS tb_tag;
DROP TABLE IF EXISTS tb_problem;
DROP TABLE IF EXISTS tb_user;

CREATE TABLE tb_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    role ENUM('STUDENT', 'TEACHER', 'ADMIN') NOT NULL DEFAULT 'STUDENT' COMMENT '用户角色',
    global_ac_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '全局AC题数',
    submit_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '提交总次数',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常，0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    KEY idx_solved_submit (global_ac_count DESC, submit_count ASC, id ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE tb_problem (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '题目ID',
    title VARCHAR(255) NOT NULL COMMENT '题目标题',
    description LONGTEXT NOT NULL COMMENT '题目描述',
    input_description TEXT DEFAULT NULL COMMENT '输入描述',
    output_description TEXT DEFAULT NULL COMMENT '输出描述',
    sample_input TEXT DEFAULT NULL COMMENT '样例输入',
    sample_output TEXT DEFAULT NULL COMMENT '样例输出',
    difficulty ENUM('EASY', 'MEDIUM', 'HARD') NOT NULL DEFAULT 'EASY' COMMENT '题目难度',
    time_limit_ms INT UNSIGNED NOT NULL DEFAULT 1000 COMMENT '时间限制，单位毫秒',
    memory_limit_kb INT UNSIGNED NOT NULL DEFAULT 262144 COMMENT '内存限制，单位KB',
    author_id BIGINT UNSIGNED DEFAULT NULL COMMENT '作者用户ID，逻辑关联tb_user.id',
    is_visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否可见：1可见，0隐藏',
    accepted_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'AC次数',
    submit_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '提交次数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_visible (is_visible, id),
    KEY idx_author_id (author_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目表';

CREATE TABLE tb_tag (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    name VARCHAR(64) NOT NULL COMMENT '标签名称',
    color VARCHAR(32) DEFAULT NULL COMMENT '标签颜色',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='算法标签表';

CREATE TABLE tb_problem_tag (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    problem_id BIGINT UNSIGNED NOT NULL COMMENT '题目ID，逻辑关联tb_problem.id',
    tag_id BIGINT UNSIGNED NOT NULL COMMENT '标签ID，逻辑关联tb_tag.id',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_problem_tag (problem_id, tag_id),
    KEY idx_tag_problem (tag_id, problem_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目-标签关联表';

CREATE TABLE tb_test_case (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '测试用例ID',
    problem_id BIGINT UNSIGNED NOT NULL COMMENT '题目ID，逻辑关联tb_problem.id',
    input_path VARCHAR(512) NOT NULL COMMENT '输入文件路径或文件名',
    output_path VARCHAR(512) NOT NULL COMMENT '输出文件路径或文件名',
    input_preview TEXT DEFAULT NULL COMMENT '输入文件前2KB预览',
    output_preview TEXT DEFAULT NULL COMMENT '输出文件前2KB预览',
    score INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '测试点分值，IOI赛制可用',
    sort_order INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '测试点排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_problem_order (problem_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试用例表';

CREATE TABLE tb_contest (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '比赛ID',
    title VARCHAR(255) NOT NULL COMMENT '比赛标题',
    description TEXT DEFAULT NULL COMMENT '比赛描述',
    rule_type ENUM('ACM', 'IOI') NOT NULL DEFAULT 'ACM' COMMENT '赛制类型',
    start_time DATETIME NOT NULL COMMENT '比赛开始时间',
    end_time DATETIME NOT NULL COMMENT '比赛结束时间',
    is_public TINYINT NOT NULL DEFAULT 1 COMMENT '是否公开：1公开，0私有',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常，0停用',
    created_by BIGINT UNSIGNED DEFAULT NULL COMMENT '创建者用户ID，逻辑关联tb_user.id',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_start_end (start_time, end_time),
    KEY idx_created_by (created_by, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛表';

CREATE TABLE tb_contest_problem (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '比赛题目关联ID',
    contest_id BIGINT UNSIGNED NOT NULL COMMENT '比赛ID，逻辑关联tb_contest.id',
    problem_id BIGINT UNSIGNED NOT NULL COMMENT '题目ID，逻辑关联tb_problem.id',
    alias VARCHAR(16) NOT NULL COMMENT '题目别名，如A、B、C',
    sort_order INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '题目排序',
    score INT UNSIGNED NOT NULL DEFAULT 100 COMMENT '题目满分或权重',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_contest_problem (contest_id, problem_id),
    UNIQUE KEY uk_contest_alias (contest_id, alias),
    KEY idx_problem_id (problem_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛题目关联表';

CREATE TABLE tb_contest_registration (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '报名ID',
    contest_id BIGINT UNSIGNED NOT NULL COMMENT '比赛ID，逻辑关联tb_contest.id',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID，逻辑关联tb_user.id',
    status ENUM('REGISTERED', 'CANCELLED') NOT NULL DEFAULT 'REGISTERED' COMMENT '报名状态',
    registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_contest_user (contest_id, user_id),
    KEY idx_user_contest (user_id, contest_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛报名表';

CREATE TABLE tb_submission (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '提交ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '提交用户ID，逻辑关联tb_user.id',
    problem_id BIGINT UNSIGNED NOT NULL COMMENT '题目ID，逻辑关联tb_problem.id',
    contest_id BIGINT UNSIGNED DEFAULT NULL COMMENT '比赛ID，逻辑关联tb_contest.id，非比赛提交为空',
    language VARCHAR(32) NOT NULL COMMENT '提交语言',
    code LONGTEXT NOT NULL COMMENT '提交代码',
    code_length INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '代码长度',
    status ENUM(
        'PENDING',
        'JUDGING',
        'COMPILING',
        'RUNNING',
        'ACCEPTED',
        'WRONG_ANSWER',
        'TIME_LIMIT_EXCEEDED',
        'MEMORY_LIMIT_EXCEEDED',
        'RUNTIME_ERROR',
        'COMPILE_ERROR',
        'SYSTEM_ERROR'
    ) NOT NULL DEFAULT 'PENDING' COMMENT '判题状态',
    score INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '得分，ACM赛制通常为0或100',
    time_used_ms INT UNSIGNED DEFAULT NULL COMMENT '运行耗时，单位毫秒',
    memory_used_kb INT UNSIGNED DEFAULT NULL COMMENT '运行内存，单位KB',
    judge_message TEXT DEFAULT NULL COMMENT '判题信息',
    error_log TEXT DEFAULT NULL COMMENT '编译或运行错误日志',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    judged_at DATETIME DEFAULT NULL COMMENT '判题完成时间',
    PRIMARY KEY (id),
    KEY idx_status (status, created_at, id),
    KEY idx_contest_user (contest_id, user_id, problem_id, created_at),
    KEY idx_user_problem (user_id, problem_id, created_at),
    KEY idx_problem_status (problem_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提交记录表';

CREATE TABLE tb_notice (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    title VARCHAR(255) NOT NULL COMMENT '公告标题',
    content LONGTEXT NOT NULL COMMENT '公告内容',
    author_id BIGINT UNSIGNED DEFAULT NULL COMMENT '作者用户ID，逻辑关联tb_user.id',
    is_pinned TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：1置顶，0不置顶',
    is_visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否可见：1可见，0隐藏',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_pinned_created (is_visible, is_pinned DESC, created_at DESC, id DESC),
    KEY idx_author_id (author_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';

CREATE TABLE tb_audit_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '审计日志ID',
    operator_id BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人用户ID，逻辑关联tb_user.id',
    action VARCHAR(128) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(64) DEFAULT NULL COMMENT '操作对象类型',
    target_id BIGINT UNSIGNED DEFAULT NULL COMMENT '操作对象ID',
    ip_address VARCHAR(64) DEFAULT NULL COMMENT '客户端IP地址',
    user_agent VARCHAR(512) DEFAULT NULL COMMENT '客户端User-Agent',
    detail JSON DEFAULT NULL COMMENT '操作详情JSON',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_operator_created (operator_id, created_at DESC),
    KEY idx_target (target_type, target_id),
    KEY idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

SET FOREIGN_KEY_CHECKS = 1;
