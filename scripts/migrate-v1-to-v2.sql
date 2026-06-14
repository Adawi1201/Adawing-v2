-- =============================================================================
-- AdaWing v1 → v2 数据迁移
-- =============================================================================
-- 前提: OSS 资源已删除，资源引用全部置空，后续通过 v2 管理端重新上传
-- 用法: mysql -u root -p adawing < migrate-v1-to-v2.sql
-- =============================================================================

-- 1. 清空 v2 已有数据（如果之前跑过 schema.sql 自动初始化了测试数据）
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE article_tag;
TRUNCATE TABLE article;
TRUNCATE TABLE tag;
TRUNCATE TABLE note;
TRUNCATE TABLE message;
TRUNCATE TABLE system_config;
TRUNCATE TABLE resource_reference;
TRUNCATE TABLE resource;
TRUNCATE TABLE review_task;
TRUNCATE TABLE sys_user;
TRUNCATE TABLE mcp_key;
SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 2. tag — 原样搬
-- =============================================================================
INSERT INTO tag (id, name, description, color, sort_order, create_time, update_time)
SELECT id, name, NULL, NULL, 0, create_time, update_time
FROM tag;

-- =============================================================================
-- 3. article — 核心数据，cover_resource_id 全部置 NULL（OSS 文件已删）
--    v1 status: 0=草稿→v2 0=DRAFT, 1=已发布→v2 2=PUBLISHED
--    v1 的 sync_note 列在 v2 已移除
-- =============================================================================
INSERT INTO article (id, title, summary, content, cover_resource_id,
                     status, source, source_agent,
                     is_top, is_hidden, view_count,
                     reject_reason, reviewer_note,
                     create_time, update_time)
SELECT
    id, title, summary, content,
    NULL AS cover_resource_id,              -- ← 封面置空，以后重新传
    CASE status WHEN 0 THEN 0 ELSE 2 END,   -- ← 草稿→0, 已发布→2
    0, NULL,                                -- source/source_agent
    is_top, is_hidden, view_count,
    NULL, NULL,
    create_time, update_time
FROM article;

-- =============================================================================
-- 4. article_tag — 原样搬
-- =============================================================================
INSERT INTO article_tag (id, article_id, tag_id, primary_tag, sort_order, create_time)
SELECT id, article_id, tag_id, 0, 0, NOW()
FROM article_tag;

-- =============================================================================
-- 5. note — 所有历史笔记都是已发布状态
-- =============================================================================
INSERT INTO note (id, title, content, type, source_id, status, create_time, update_time)
SELECT id, title, content, type, source_id, 2, create_time, update_time
FROM note;

-- =============================================================================
-- 6. message — avatar_resource_id 置 NULL（OSS 文件已删）
-- =============================================================================
INSERT INTO message (id, nickname, email, avatar_resource_id,
                     content, status, reply, reject_reason, like_count,
                     ref_type, ref_id, ref_title,
                     create_time, update_time)
SELECT
    id, nickname, email,
    NULL AS avatar_resource_id,             -- ← 头像置空
    content, status, reply, reject_reason, like_count,
    0, NULL, NULL,
    create_time, update_time
FROM message;

-- =============================================================================
-- 7. system_config — 原样搬
-- =============================================================================
INSERT INTO system_config (id, config_key, config_value, description, create_time, update_time)
SELECT id, config_key, config_value, description, create_time, update_time
FROM system_config;

-- =============================================================================
-- 8. 默认管理员（v1 没有用户表，v2 手动建一个）
--    密码: admin123
-- =============================================================================
INSERT INTO sys_user (id, username, password_hash, nickname, email, status)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Ada', NULL, 1);

-- =============================================================================
-- 9. 矫正所有表自增序列
-- =============================================================================
ALTER TABLE tag            AUTO_INCREMENT = 100;
ALTER TABLE article        AUTO_INCREMENT = 100;
ALTER TABLE article_tag    AUTO_INCREMENT = 100;
ALTER TABLE note           AUTO_INCREMENT = 100;
ALTER TABLE message        AUTO_INCREMENT = 100;
ALTER TABLE system_config  AUTO_INCREMENT = 100;
ALTER TABLE sys_user       AUTO_INCREMENT = 100;

-- =============================================================================
-- 验证
-- =============================================================================
SELECT 'article' AS tbl, COUNT(*) AS cnt FROM article
UNION ALL SELECT 'tag',      COUNT(*) FROM tag
UNION ALL SELECT 'article_tag', COUNT(*) FROM article_tag
UNION ALL SELECT 'note',     COUNT(*) FROM note
UNION ALL SELECT 'message',  COUNT(*) FROM message
UNION ALL SELECT 'system_config', COUNT(*) FROM system_config
UNION ALL SELECT 'sys_user', COUNT(*) FROM sys_user;
