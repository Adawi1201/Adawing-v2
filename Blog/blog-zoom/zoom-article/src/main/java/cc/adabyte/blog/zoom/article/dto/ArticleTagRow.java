package cc.adabyte.blog.zoom.article.dto;

/**
 * 批量查询文章标签时的行投影：article_id + 标签展示字段。
 */
public record ArticleTagRow(Long articleId, Long tagId, String name, String color) {}
