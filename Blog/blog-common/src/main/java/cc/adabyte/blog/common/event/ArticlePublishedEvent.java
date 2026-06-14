package cc.adabyte.blog.common.event;

import java.time.LocalDateTime;

public record ArticlePublishedEvent(Long articleId, String title, String summary, LocalDateTime publishTime) {}
