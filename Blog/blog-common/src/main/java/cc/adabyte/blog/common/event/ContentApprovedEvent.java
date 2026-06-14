package cc.adabyte.blog.common.event;

public record ContentApprovedEvent(Long contentId, String contentType, String reviewerNote, Long coverResourceId) {}
