package cc.adabyte.blog.system.review.strategy;

import cc.adabyte.blog.common.event.ContentApprovedEvent;
import cc.adabyte.blog.common.event.ContentRejectedEvent;
import cc.adabyte.blog.common.strategy.ReviewStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageReviewStrategy implements ReviewStrategy {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public boolean supports(String contentType) {
        return "message".equals(contentType);
    }

    @Override
    public void onApprove(Long contentId, String reviewerNote, Long coverResourceId, Long avatarResourceId) {
        eventPublisher.publishEvent(new ContentApprovedEvent(contentId, "message", reviewerNote, null, avatarResourceId));
    }

    @Override
    public void onReject(Long contentId, String reason, String reviewerNote) {
        eventPublisher.publishEvent(new ContentRejectedEvent(contentId, "message", reason, reviewerNote));
    }
}
