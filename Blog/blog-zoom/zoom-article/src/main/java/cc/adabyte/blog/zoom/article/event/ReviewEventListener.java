package cc.adabyte.blog.zoom.article.event;

import cc.adabyte.blog.common.event.ContentApprovedEvent;
import cc.adabyte.blog.common.event.ContentRejectedEvent;
import cc.adabyte.blog.zoom.article.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component("articleReviewEventListener")
@RequiredArgsConstructor
public class ReviewEventListener {

    private final ArticleService articleService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onContentApproved(ContentApprovedEvent event) {
        if (!"article".equals(event.contentType())) return;
        log.info("[Article] 审核通过: articleId={}, coverResourceId={}", event.contentId(), event.coverResourceId());
        try {
            if (event.coverResourceId() != null) {
                articleService.updateCover(event.contentId(), event.coverResourceId());
                log.info("[Article] 已设置封面: articleId={}, resourceId={}", event.contentId(), event.coverResourceId());
            }
            articleService.publish(event.contentId());
        } catch (Exception e) {
            log.error("[Article] 审核通过后发布文章失败: articleId={}", event.contentId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onContentRejected(ContentRejectedEvent event) {
        if (!"article".equals(event.contentType())) return;
        log.info("[Article] 审核拒绝: articleId={}, reason={}", event.contentId(), event.reason());
        try {
            articleService.reject(event.contentId(), event.reason(), event.reviewerNote());
        } catch (Exception e) {
            log.error("[Article] 审核拒绝后更新文章失败: articleId={}", event.contentId(), e);
        }
    }
}
