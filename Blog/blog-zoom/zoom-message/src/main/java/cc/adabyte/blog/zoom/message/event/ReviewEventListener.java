package cc.adabyte.blog.zoom.message.event;

import cc.adabyte.blog.common.event.ContentApprovedEvent;
import cc.adabyte.blog.common.event.ContentRejectedEvent;
import cc.adabyte.blog.zoom.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component("messageReviewEventListener")
@RequiredArgsConstructor
public class ReviewEventListener {

    private final MessageService messageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onContentApproved(ContentApprovedEvent event) {
        if (!"message".equals(event.contentType())) return;
        log.info("[Message] 审核通过: messageId={}, avatarResourceId={}", event.contentId(), event.avatarResourceId());
        try {
            messageService.approve(event.contentId(), event.reviewerNote(), event.avatarResourceId());
        } catch (Exception e) {
            log.error("[Message] 审核通过后更新留言失败: messageId={}", event.contentId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onContentRejected(ContentRejectedEvent event) {
        if (!"message".equals(event.contentType())) return;
        log.info("[Message] 审核拒绝: messageId={}, reason={}", event.contentId(), event.reason());
        try {
            messageService.reject(event.contentId(), event.reason());
        } catch (Exception e) {
            log.error("[Message] 审核拒绝后更新留言失败: messageId={}", event.contentId(), e);
        }
    }
}
