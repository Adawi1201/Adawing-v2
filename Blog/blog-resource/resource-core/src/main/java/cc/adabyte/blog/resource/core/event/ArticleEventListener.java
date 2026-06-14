package cc.adabyte.blog.resource.core.event;

import cc.adabyte.blog.common.event.ArticleDeletedEvent;
import cc.adabyte.blog.resource.core.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleEventListener {

    private final ResourceService resourceService;

    @EventListener
    public void onArticleDeleted(ArticleDeletedEvent event) {
        log.info("[Resource] 文章已删除，解绑资源: articleId={}", event.articleId());
        resourceService.unbindByObject("article", event.articleId());
    }
}
