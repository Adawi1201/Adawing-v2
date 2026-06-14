-- 从 v1 dump 提取所有资源 URL + 来源信息，按用途分类

SELECT 'ARTICLE_COVER' AS type, id AS owner_id, title AS label, cover_url AS url
FROM article WHERE cover_url IS NOT NULL

UNION ALL

SELECT 'MESSAGE_AVATAR', id, nickname, avatar
FROM message WHERE avatar IS NOT NULL

UNION ALL

SELECT 'PROFILE_AVATAR', id, nickname, avatar
FROM profile WHERE avatar IS NOT NULL

UNION ALL

SELECT 'EMOJI', id, name, url
FROM emoji

UNION ALL

SELECT 'AVATAR_POOL', id, CONCAT('avatar-', id), url
FROM avatar

UNION ALL

SELECT 'FILE_RECORD', id, original_name, url
FROM file_record

ORDER BY type, owner_id;
