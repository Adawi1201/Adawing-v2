package cc.adabyte.blog.system.review.strategy;

import cc.adabyte.blog.common.event.ContentApprovedEvent;
import cc.adabyte.blog.common.event.ContentRejectedEvent;
import cc.adabyte.blog.common.strategy.ReviewStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NoteReviewStrategy implements ReviewStrategy {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public boolean supports(String contentType) {
        return "note".equals(contentType);
    }

    @Override
    public void onApprove(Long contentId, String reviewerNote, Long coverResourceId, Long avatarResourceId) {
        eventPublisher.publishEvent(new ContentApprovedEvent(contentId, "note", reviewerNote, null, null));
    }

    @Override
    public void onReject(Long contentId, String reason, String reviewerNote) {
        eventPublisher.publishEvent(new ContentRejectedEvent(contentId, "note", reason, reviewerNote));
    }
}
