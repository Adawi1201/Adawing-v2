package cc.adabyte.blog.zoom.article.dto;

/**
 * 文章附带的轻量标签视图（仅展示所需字段）。
 */
public record ArticleTagView(Long id, String name, String color) {}
