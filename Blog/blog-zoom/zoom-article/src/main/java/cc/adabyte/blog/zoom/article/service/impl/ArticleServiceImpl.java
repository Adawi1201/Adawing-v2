package cc.adabyte.blog.zoom.article.service.impl;

import cc.adabyte.blog.common.event.ArticleDeletedEvent;
import cc.adabyte.blog.common.event.ArticlePublishedEvent;
import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.resource.core.service.ResourceAllocationFacade;
import cc.adabyte.blog.zoom.article.entity.Article;
import cc.adabyte.blog.zoom.article.entity.ArticleTag;
import cc.adabyte.blog.zoom.article.mapper.ArticleMapper;
import cc.adabyte.blog.zoom.article.mapper.ArticleTagMapper;
import cc.adabyte.blog.zoom.article.service.ArticleService;
import cc.adabyte.blog.zoom.shared.enums.ArticleSource;
import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ResourceAllocationFacade resourceFacade;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PageResult<Article> listPublished(int page, int size) {
        Page<Article> mpPage = new Page<>(page, size);
        var result = articleMapper.selectPublished(mpPage, ContentStatus.PUBLISHED.getValue());
        return PageResult.of(result.getTotal(), result.getRecords(), result.getCurrent(), result.getSize());
    }

    @Override
    public Article getPublishedById(Long id) {
        return articleMapper.selectPublishedById(id, ContentStatus.PUBLISHED.getValue());
    }

    @Override
    public PageResult<Article> listByTag(String tagName, int page, int size) {
        Page<Article> mpPage = new Page<>(page, size);
        var result = articleMapper.selectByTag(mpPage, tagName, ContentStatus.PUBLISHED.getValue());
        return PageResult.of(result.getTotal(), result.getRecords(), result.getCurrent(), result.getSize());
    }

    @Override
    public Map<String, List<Article>> listArchive() {
        List<Article> articles = articleMapper.selectPublishedAll(ContentStatus.PUBLISHED.getValue());
        return articles.stream()
                .collect(Collectors.groupingBy(
                        a -> YearMonth.from(a.getCreateTime()).toString(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    @Override
    public PageResult<Article> listAll(int page, int size) {
        Page<Article> mpPage = new Page<>(page, size);
        var result = articleMapper.selectAll(mpPage);
        return PageResult.of(result.getTotal(), result.getRecords(), result.getCurrent(), result.getSize());
    }

    @Override
    public Article getAdminById(Long id) {
        return articleMapper.selectAdminById(id);
    }

    @Override
    public long countByStatus(ContentStatus status) {
        Integer value = status != null ? status.getValue() : null;
        Long count = articleMapper.countByStatus(value);
        return count != null ? count : 0L;
    }

    @Override
    public long countBySource(ArticleSource source) {
        Long count = articleMapper.countBySource(source.getValue());
        return count != null ? count : 0L;
    }

    @Override
    @Transactional
    public void saveOrUpdate(Article article, List<String> tagNames) {
        if (article.getStatus() == null) {
            article.setStatus(ContentStatus.DRAFT);
        }
        if (article.getSource() == null) {
            article.setSource(ArticleSource.ORIGINAL);
        }
        if (article.getHidden() == null) {
            article.setHidden(false);
        }
        if (article.getTop() == null) {
            article.setTop(false);
        }
        if (article.getViewCount() == null) {
            article.setViewCount(0);
        }
        if (article.getId() == null) {
            articleMapper.insert(article);
        } else {
            articleMapper.updateById(article);
            articleTagMapper.deleteByArticleId(article.getId());
            resourceFacade.unbindArticleResources(article.getId());
        }
        resourceFacade.bindArticleResources(article.getId(), article.getCoverResourceId(), article.getContent());
    }

    @Override
    @Transactional
    public void publish(Long id) {
        updateStatusIfPresent(id, ContentStatus.PUBLISHED);
        Article article = articleMapper.selectById(id);
        if (article != null) {
            eventPublisher.publishEvent(new ArticlePublishedEvent(
                    article.getId(), article.getTitle(), article.getSummary(), article.getUpdateTime()));
        }
    }

    @Override
    @Transactional
    public void reject(Long id, String reason, String reviewerNote) {
        Article article = articleMapper.selectById(id);
        if (article != null) {
            // 闭环：拒绝后回到草稿状态，可修改后重新提交审核
            article.setStatus(ContentStatus.DRAFT);
            article.setRejectReason(reason);
            article.setReviewerNote(reviewerNote);
            articleMapper.updateById(article);
            log.info("[Article] Rejected → DRAFT: articleId={}, reason={}", id, reason);
        }
    }

    @Override
    @Transactional
    public void submitForReview(Long id) {
        updateStatusIfPresent(id, ContentStatus.PENDING_REVIEW);
    }

    private void updateStatusIfPresent(Long id, ContentStatus status) {
        Article article = articleMapper.selectById(id);
        if (article != null) {
            article.setStatus(status);
            article.setUpdateTime(LocalDateTime.now());
            articleMapper.updateById(article);
        }
    }

    @Override
    @Transactional
    public void hide(Long id) {
        Article article = articleMapper.selectById(id);
        if (article != null) {
            article.setHidden(true);
            articleMapper.updateById(article);
        }
    }

    @Override
    @Transactional
    public void updateCover(Long id, Long coverResourceId) {
        Article article = articleMapper.selectById(id);
        if (article != null) {
            article.setCoverResourceId(coverResourceId);
            articleMapper.updateById(article);
            log.info("[Article] Cover updated: articleId={}, resourceId={}", id, coverResourceId);
        }
    }

    @Override
    @Transactional
    public void deleteDraft(Long id) {
        Article article = articleMapper.selectById(id);
        if (article != null && article.getStatus() == ContentStatus.DRAFT) {
            articleMapper.deleteById(id);
            eventPublisher.publishEvent(new ArticleDeletedEvent(id));
        }
    }

    @Override
    public Long getTotalViewCount() {
        Long total = articleMapper.selectTotalViewCount();
        return total != null ? total : 0L;
    }

    @Override
    public void syncViewCountsToDb() {
        throw new UnsupportedOperationException("待缓存层就绪后实现");
    }
}
