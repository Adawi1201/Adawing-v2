package cc.adabyte.blog.common.event;

public record ContentRejectedEvent(Long contentId, String contentType, String reason, String reviewerNote) {}
