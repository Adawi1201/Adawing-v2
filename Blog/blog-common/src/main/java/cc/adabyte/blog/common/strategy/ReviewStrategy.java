package cc.adabyte.blog.common.strategy;

public interface ReviewStrategy {
    boolean supports(String contentType);
    void onApprove(Long contentId, String reviewerNote, Long coverResourceId);
    void onReject(Long contentId, String reason, String reviewerNote);
}
