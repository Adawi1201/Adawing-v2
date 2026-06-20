package cc.adabyte.blog.zoom.article.service.impl;

import cc.adabyte.blog.common.event.ArticlePublishedEvent;
import cc.adabyte.blog.common.exception.BusinessException;
import cc.adabyte.blog.resource.core.service.ResourceAllocationFacade;
import cc.adabyte.blog.zoom.article.entity.Article;
import cc.adabyte.blog.zoom.article.mapper.ArticleMapper;
import cc.adabyte.blog.zoom.article.mapper.ArticleTagMapper;
import cc.adabyte.blog.zoom.shared.enums.ArticleSource;
import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceImplPublishTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private ArticleTagMapper articleTagMapper;

    @Mock
    private ResourceAllocationFacade resourceFacade;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ArticleServiceImpl articleService;

    @Test
    void originalArticleCanBePublishedDirectly() {
        Article article = new Article();
        article.setId(1L);
        article.setSource(ArticleSource.ORIGINAL);
        article.setStatus(ContentStatus.DRAFT);
        when(articleMapper.selectById(1L)).thenReturn(article);

        articleService.publish(1L);

        assertEquals(ContentStatus.PUBLISHED, article.getStatus());
        verify(articleMapper).updateById(any(Article.class));
        verify(eventPublisher).publishEvent(any(ArticlePublishedEvent.class));
    }

    @Test
    void aiGeneratedDraftCannotBePublishedDirectly() {
        Article article = new Article();
        article.setId(2L);
        article.setSource(ArticleSource.AI_GENERATED);
        article.setStatus(ContentStatus.DRAFT);
        when(articleMapper.selectById(2L)).thenReturn(article);

        BusinessException ex = assertThrows(BusinessException.class, () -> articleService.publish(2L));
        assertEquals("Agent 生成文章必须先提交审核并通过后方可发布", ex.getMessage());

        verify(articleMapper, never()).updateById(any(Article.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void aiGeneratedPendingReviewArticleCanBePublished() {
        Article article = new Article();
        article.setId(3L);
        article.setSource(ArticleSource.AI_GENERATED);
        article.setStatus(ContentStatus.PENDING_REVIEW);
        when(articleMapper.selectById(3L)).thenReturn(article);

        articleService.publish(3L);

        assertEquals(ContentStatus.PUBLISHED, article.getStatus());
        verify(articleMapper).updateById(any(Article.class));
        verify(eventPublisher).publishEvent(any(ArticlePublishedEvent.class));
    }
}
