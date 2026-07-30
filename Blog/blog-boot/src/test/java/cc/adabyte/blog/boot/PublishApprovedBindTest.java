package cc.adabyte.blog.boot;

import cc.adabyte.blog.resource.core.service.ResourceAllocationFacade;
import cc.adabyte.blog.zoom.article.entity.Article;
import cc.adabyte.blog.zoom.article.mapper.ArticleMapper;
import cc.adabyte.blog.zoom.article.mapper.ArticleTagMapper;
import cc.adabyte.blog.zoom.article.service.ViewCountBuffer;
import cc.adabyte.blog.zoom.article.service.impl.ArticleServiceImpl;
import cc.adabyte.blog.zoom.tag.service.TagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.Mockito.*;

/**
 * 审核发布资源绑定回归测试。
 *
 * <p>线上事故：审核通过路径（updateCover + publishApproved）只写字段和状态，
 * 不做资源绑定。审核时指定的封面与正文图片 refCount 保持 0，ARTICLE 池资源
 * 对匿名访客 404，需手动编辑文章触发 saveOrUpdate 才恢复。
 * 修复后 publishApproved 发布前补齐绑定（bindArticleResources 幂等）。
 */
@DisplayName("审核发布资源绑定测试")
class PublishApprovedBindTest {

    @Test
    @DisplayName("publishApproved 应按当前封面与正文补齐资源绑定")
    void publishApprovedBindsResources() {
        ArticleMapper articleMapper = mock(ArticleMapper.class);
        ResourceAllocationFacade resourceFacade = mock(ResourceAllocationFacade.class);
        ArticleServiceImpl service = new ArticleServiceImpl(
                articleMapper,
                mock(ArticleTagMapper.class),
                resourceFacade,
                mock(ApplicationEventPublisher.class),
                mock(ViewCountBuffer.class),
                mock(TagService.class));

        Article article = new Article();
        article.setId(12L);
        article.setCoverResourceId(39L);
        article.setContent("正文 ![图](resource://40)");
        when(articleMapper.selectById(12L)).thenReturn(article);

        service.publishApproved(12L);

        verify(resourceFacade).bindArticleResources(12L, 39L, "正文 ![图](resource://40)");
    }

    @Test
    @DisplayName("文章不存在时不触发绑定")
    void publishApprovedSkipsMissingArticle() {
        ArticleMapper articleMapper = mock(ArticleMapper.class);
        ResourceAllocationFacade resourceFacade = mock(ResourceAllocationFacade.class);
        ArticleServiceImpl service = new ArticleServiceImpl(
                articleMapper,
                mock(ArticleTagMapper.class),
                resourceFacade,
                mock(ApplicationEventPublisher.class),
                mock(ViewCountBuffer.class),
                mock(TagService.class));

        when(articleMapper.selectById(99L)).thenReturn(null);

        service.publishApproved(99L);

        verifyNoInteractions(resourceFacade);
    }
}
