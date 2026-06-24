ALTER TABLE tb_notice
    ADD CONSTRAINT chk_notice_flags CHECK (is_pinned IN (0, 1) AND is_visible IN (0, 1));
