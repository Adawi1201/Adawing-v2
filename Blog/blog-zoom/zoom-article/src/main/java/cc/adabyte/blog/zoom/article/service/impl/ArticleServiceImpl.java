package cc.adabyte.blog.zoom.article.service.impl;

import cc.adabyte.blog.common.event.ArticleDeletedEvent;
import cc.adabyte.blog.common.event.ArticlePublishedEvent;
import cc.adabyte.blog.common.exception.BusinessException;
import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.resource.core.service.ResourceAllocationFacade;
import cc.adabyte.blog.zoom.article.dto.ArticleTagRow;
import cc.adabyte.blog.zoom.article.dto.ArticleTagView;
import cc.adabyte.blog.zoom.article.dto.TagWithCount;
import cc.adabyte.blog.zoom.article.entity.Article;
import cc.adabyte.blog.zoom.article.entity.ArticleTag;
import cc.adabyte.blog.zoom.article.mapper.ArticleMapper;
import cc.adabyte.blog.zoom.article.mapper.ArticleTagMapper;
import cc.adabyte.blog.zoom.article.service.ArticleService;
import cc.adabyte.blog.zoom.article.service.ViewCountBuffer;
import cc.adabyte.blog.zoom.shared.enums.ArticleSource;
import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import cc.adabyte.blog.zoom.tag.entity.Tag;
import cc.adabyte.blog.zoom.tag.service.TagService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
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
    private final ViewCountBuffer viewCountBuffer;
    private final TagService tagService;

    @Override
    public PageResult<Article> listPublished(int page, int size) {
        Page<Article> mpPage = new Page<>(page, size);
        var result = articleMapper.selectPublished(mpPage, ContentStatus.PUBLISHED.getValue());
        attachTags(result.getRecords());
        return PageResult.of(result.getTotal(), result.getRecords(), result.getCurrent(), result.getSize());
    }

    @Override
    public Article getPublishedById(Long id) {
        Article article = articleMapper.selectPublishedById(id, ContentStatus.PUBLISHED.getValue());
        if (article == null) {
            return null;
        }
        // 内存累加，不直接写库；由定时任务批量刷回
        viewCountBuffer.increment(id);
        // 返回值 = DB 基数 + 未刷回增量，保证展示实时
        article.setViewCount(article.getViewCount() + (int) viewCountBuffer.peek(id));
        attachTags(List.of(article));
        return article;
    }

    @Override
    public PageResult<Article> listByTag(String tagName, int page, int size) {
        Page<Article> mpPage = new Page<>(page, size);
        var result = articleMapper.selectByTag(mpPage, tagName, ContentStatus.PUBLISHED.getValue());
        attachTags(result.getRecords());
        return PageResult.of(result.getTotal(), result.getRecords(), result.getCurrent(), result.getSize());
    }

    /** 批量为文章装配标签，避免 N+1。 */
    private void attachTags(List<Article> articles) {
        if (articles == null || articles.isEmpty()) return;
        List<Long> ids = articles.stream().map(Article::getId).toList();
        List<ArticleTagRow> rows = articleTagMapper.selectTagViewsByArticleIds(ids);
        Map<Long, List<ArticleTagView>> byArticle = new LinkedHashMap<>();
        for (ArticleTagRow row : rows) {
            byArticle.computeIfAbsent(row.articleId(), k -> new ArrayList<>())
                    .add(new ArticleTagView(row.tagId(), row.name(), row.color()));
        }
        for (Article article : articles) {
            article.setTags(byArticle.getOrDefault(article.getId(), List.of()));
        }
    }

    @Override
    public List<TagWithCount> listTagsWithCount() {
        return tagService.list().stream()
                .map(t -> {
                    Long c = articleTagMapper.countByTagId(t.getId());
                    return new TagWithCount(t.getId(), t.getName(), t.getDescription(), t.getColor(),
                            c == null ? 0L : c);
                })
                .toList();
    }

    @Override
    @Transactional
    public void mergeTags(Long sourceTagId, Long targetTagId) {
        if (sourceTagId == null || targetTagId == null || sourceTagId.equals(targetTagId)) {
            throw new BusinessException("源标签与目标标签不能为空或相同");
        }
        // 迁移文章关联：先删会撞唯一对的源关联，再把其余源关联改指目标
        articleTagMapper.deleteConflictingSourceLinks(sourceTagId, targetTagId);
        articleTagMapper.repointRemaining(sourceTagId, targetTagId);
        // 源标签词条删除
        tagService.deleteById(sourceTagId);
    }

    @Override
    @Transactional
    public void deleteTag(Long tagId) {
        Long c = articleTagMapper.countByTagId(tagId);
        if (c != null && c > 0) {
            throw new BusinessException("该标签仍被 " + c + " 篇文章引用，无法删除");
        }
        tagService.deleteById(tagId);
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
        Article article = articleMapper.selectAdminById(id);
        if (article != null) {
            attachTags(List.of(article));
        }
        return article;
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
        bindTags(article.getId(), tagNames);
        resourceFacade.bindArticleResources(article.getId(), article.getCoverResourceId(), article.getContent());
    }

    /** 按名解析标签词条并写入 article_tag（唯一对约束去重）。 */
    private void bindTags(Long articleId, List<String> tagNames) {
        if (articleId == null || tagNames == null || tagNames.isEmpty()) return;
        List<Tag> tags = tagService.resolveByNames(tagNames);
        int order = 0;
        for (Tag tag : tags) {
            ArticleTag link = new ArticleTag();
            link.setArticleId(articleId);
            link.setTagId(tag.getId());
            link.setPrimary(order == 0);
            link.setSortOrder(order++);
            articleTagMapper.insert(link);
        }
    }

    @Override
    @Transactional
    public void publish(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            return;
        }
        if (article.getSource() == ArticleSource.AI_GENERATED) {
            throw new BusinessException("Agent 生成文章由审核通过后自动发布，不支持手动发布");
        }
        doPublish(article);
    }

    @Override
    @Transactional
    public void publishApproved(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            return;
        }
        doPublish(article);
    }

    private void doPublish(Article article) {
        article.setStatus(ContentStatus.PUBLISHED);
        article.setUpdateTime(LocalDateTime.now());
        articleMapper.updateById(article);
        eventPublisher.publishEvent(new ArticlePublishedEvent(
                article.getId(), article.getTitle(), article.getSummary(), article.getUpdateTime()));
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
    public void delete(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            return;
        }
        articleTagMapper.deleteByArticleId(id);
        articleMapper.deleteById(id);
        eventPublisher.publishEvent(new ArticleDeletedEvent(id));
    }

    @Override
    public Long getTotalViewCount() {
        Long total = articleMapper.selectTotalViewCount();
        return total != null ? total : 0L;
    }

    @Override
    public void syncViewCountsToDb() {
        Map<Long, Long> deltas = viewCountBuffer.drain();
        if (deltas.isEmpty()) {
            return;
        }
        deltas.forEach(articleMapper::incrementViewCountBy);
        log.info("[ViewCount] 已刷回 {} 篇文章的浏览量增量", deltas.size());
    }
}
