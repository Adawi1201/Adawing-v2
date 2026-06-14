package cc.adabyte.blog.common.event;

public record ResourceOrphanedEvent(Long resourceId, String url) {}
