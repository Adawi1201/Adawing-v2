package cc.adabyte.blog.zoom.article.service;

import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.zoom.article.entity.Article;
import cc.adabyte.blog.zoom.shared.enums.ArticleSource;
import cc.adabyte.blog.zoom.shared.enums.ContentStatus;

import java.util.List;
import java.util.Map;

public interface ArticleService {
    PageResult<Article> listPublished(int page, int size);
    Article getPublishedById(Long id);
    PageResult<Article> listByTag(String tagName, int page, int size);
    Map<String, List<Article>> listArchive();
    PageResult<Article> listAll(int page, int size);
    Article getAdminById(Long id);
    long countByStatus(ContentStatus status);
    long countBySource(ArticleSource source);
    void saveOrUpdate(Article article, List<String> tagNames);
    void publish(Long id);
    void publishApproved(Long id);
    void submitForReview(Long id);
    void reject(Long id, String reason, String reviewerNote);
    void hide(Long id);
    void updateCover(Long id, Long coverResourceId);
    void delete(Long id);
    Long getTotalViewCount();
    void syncViewCountsToDb();
}
