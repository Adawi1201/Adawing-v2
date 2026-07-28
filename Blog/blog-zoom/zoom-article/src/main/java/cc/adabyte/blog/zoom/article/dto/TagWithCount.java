package cc.adabyte.blog.zoom.article.dto;

/**
 * 标签及其文章引用数（管理端标签列表用）。
 */
public record TagWithCount(Long id, String name, String description, String color, long articleCount) {}
