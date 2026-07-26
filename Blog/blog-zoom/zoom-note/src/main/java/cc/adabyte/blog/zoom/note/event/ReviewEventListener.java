package cc.adabyte.blog.zoom.note.event;

import cc.adabyte.blog.common.event.ContentApprovedEvent;
import cc.adabyte.blog.common.event.ContentRejectedEvent;
import cc.adabyte.blog.zoom.note.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component("noteReviewEventListener")
@RequiredArgsConstructor
public class ReviewEventListener {

    private final NoteService noteService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onContentApproved(ContentApprovedEvent event) {
        if (!"note".equals(event.contentType())) return;
        log.info("[Note] 审核通过: noteId={}", event.contentId());
        try {
            noteService.publishApproved(event.contentId());
        } catch (Exception e) {
            log.error("[Note] 审核通过后发布失败: noteId={}", event.contentId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onContentRejected(ContentRejectedEvent event) {
        if (!"note".equals(event.contentType())) return;
        log.info("[Note] 审核拒绝: noteId={}, reason={}", event.contentId(), event.reason());
        try {
            noteService.reject(event.contentId(), event.reason(), event.reviewerNote());
        } catch (Exception e) {
            log.error("[Note] 审核拒绝后更新失败: noteId={}", event.contentId(), e);
        }
    }
}
