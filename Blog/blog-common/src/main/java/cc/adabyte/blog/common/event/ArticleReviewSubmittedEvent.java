package cc.adabyte.blog.common.event;

public record ArticleReviewSubmittedEvent(Long reviewTaskId, String contentType, Long contentId) {}
