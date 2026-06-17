package cc.adabyte.blog.system.review.event;

import cc.adabyte.blog.common.event.ArticleDeletedEvent;
import cc.adabyte.blog.system.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleDeletedEventListener {

    private final ReviewService reviewService;

    @EventListener
    public void onArticleDeleted(ArticleDeletedEvent event) {
        reviewService.deleteByContent("article", event.articleId());
    }
}
