-- =============================================================================
-- AdaWing v2 Production Schema
-- =============================================================================
-- 兼容: H2 (MySQL Mode) 与 MySQL 8.0+
-- 字符集: 生产环境 utf8mb4, H2 自动兼容
-- 状态机: DRAFT → PENDING_REVIEW → PUBLISHED (or DRAFT on reject)
-- =============================================================================

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password_hash VARCHAR(128) NOT NULL COMMENT '密码哈希（BCrypt）',
    nickname VARCHAR(64) COMMENT '显示昵称',
    email VARCHAR(128) COMMENT '邮箱',
    avatar_resource_id BIGINT COMMENT '头像资源ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    role TINYINT NOT NULL DEFAULT 0 COMMENT '0管理员',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_user_username (username)
) COMMENT='系统用户';

CREATE TABLE IF NOT EXISTS mcp_key (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '密钥ID',
    name VARCHAR(64) NOT NULL COMMENT '标识名',
    key_hash VARCHAR(64) NOT NULL COMMENT 'SHA-256 哈希',
    enabled TINYINT NOT NULL DEFAULT 1,
    description VARCHAR(256) COMMENT '描述',
    last_used_at DATETIME COMMENT '最后使用时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_mcp_key_name (name)
) COMMENT='MCP 访问密钥';

CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
    config_key VARCHAR(64) NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    description VARCHAR(256) COMMENT '描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_system_config_key (config_key)
) COMMENT='系统配置';

CREATE TABLE IF NOT EXISTS tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
    name VARCHAR(64) NOT NULL COMMENT '标签名称',
    description VARCHAR(256) COMMENT '标签描述',
    color VARCHAR(7) COMMENT '十六进制色值',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tag_name (name),
    INDEX idx_tag_sort_order (sort_order)
) COMMENT='文章标签';

CREATE TABLE IF NOT EXISTS article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文章ID',
    title VARCHAR(256) NOT NULL COMMENT '标题',
    summary VARCHAR(512) COMMENT '摘要',
    content LONGTEXT NOT NULL COMMENT '正文（Markdown）',
    cover_resource_id BIGINT COMMENT '封面图资源ID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0:DRAFT 1:PENDING_REVIEW 2:PUBLISHED 4:HIDDEN (3:REJECTED not used, returns to DRAFT)',
    source TINYINT NOT NULL DEFAULT 0 COMMENT '0原创 1Agent生成',
    source_agent VARCHAR(32) COMMENT '来源Agent',
    is_top TINYINT NOT NULL DEFAULT 0 COMMENT '0普通 1置顶',
    is_hidden TINYINT NOT NULL DEFAULT 0 COMMENT '0显示 1隐藏',
    view_count INT NOT NULL DEFAULT 0 COMMENT '阅读量',
    reject_reason VARCHAR(512) COMMENT '拒绝理由',
    reviewer_note VARCHAR(512) COMMENT '审核备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_article_status_hidden (status, is_hidden),
    INDEX idx_article_source (source),
    INDEX idx_article_create_time (create_time)
) COMMENT='文章';

CREATE TABLE IF NOT EXISTS article_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    article_id BIGINT NOT NULL COMMENT '文章ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    primary_tag TINYINT NOT NULL DEFAULT 0 COMMENT '是否主要标签',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_article_tag_pair (article_id, tag_id),
    INDEX idx_article_tag_tag_id (tag_id)
) COMMENT='文章-标签关联';

CREATE TABLE IF NOT EXISTS note (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '笔记ID',
    title VARCHAR(256) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '内容（Markdown）',
    type TINYINT NOT NULL DEFAULT 0 COMMENT '0个人动态 1科技动态',
    source_id BIGINT COMMENT '来源ID（关联文章ID）',
    status TINYINT NOT NULL DEFAULT 2 COMMENT '2已发布',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_note_type_time (type, create_time)
) COMMENT='动态笔记';

CREATE TABLE IF NOT EXISTS message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '留言ID',
    nickname VARCHAR(64) NOT NULL COMMENT '昵称',
    email VARCHAR(128) NOT NULL COMMENT '邮箱',
    avatar_resource_id BIGINT COMMENT '头像资源ID',
    content TEXT NOT NULL COMMENT '内容',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待审核 1已通过 2已拒绝',
    reply TEXT COMMENT '管理员回复',
    reject_reason VARCHAR(512) COMMENT '拒绝理由',
    like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    ref_type TINYINT NOT NULL DEFAULT 0 COMMENT '0无引用 1引用文章 2引用笔记',
    ref_id BIGINT COMMENT '引用对象ID',
    ref_title VARCHAR(256) COMMENT '引用对象标题（冗余）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_message_status (status),
    INDEX idx_message_create_time (create_time)
) COMMENT='留言';

CREATE TABLE IF NOT EXISTS resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '资源ID',
    original_name VARCHAR(256) NOT NULL COMMENT '原始文件名',
    url VARCHAR(512) NOT NULL COMMENT '访问 URL',
    size BIGINT COMMENT '文件大小（字节）',
    mime_type VARCHAR(64) COMMENT 'MIME 类型',
    pool VARCHAR(16) NOT NULL DEFAULT 'MISC' COMMENT '资源池：AVATAR/ARTICLE/EMOJI/MISC',
    ref_count INT NOT NULL DEFAULT 0 COMMENT '引用计数',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/ORPHANED/DELETED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    orphaned_at DATETIME COMMENT '标记为孤儿资源时间',
    INDEX idx_resource_pool (pool),
    INDEX idx_resource_status_orphaned (status, orphaned_at)
) COMMENT='资源文件';

CREATE TABLE IF NOT EXISTS resource_reference (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '引用ID',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    module VARCHAR(32) NOT NULL COMMENT '模块名',
    object_id BIGINT NOT NULL COMMENT '业务对象ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_resource_ref_module_object (module, object_id)
) COMMENT='资源引用关系';

CREATE TABLE IF NOT EXISTS review_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '审核任务ID',
    content_type VARCHAR(32) NOT NULL COMMENT 'article / note',
    content_id BIGINT NOT NULL COMMENT '关联内容ID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待审核 1已通过 2已拒绝',
    submitter_type VARCHAR(32) COMMENT 'agent / user',
    submitter_id VARCHAR(64) COMMENT 'Agent名称或用户ID',
    reject_reason VARCHAR(512) COMMENT '拒绝理由',
    reviewer_note VARCHAR(512) COMMENT '审核备注',
    submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    review_time DATETIME COMMENT '审核时间',
    INDEX idx_review_task_status (status),
    INDEX idx_review_task_content (content_type, content_id),
    INDEX idx_review_task_submit_time (submit_time)
) COMMENT='内容审核任务';
