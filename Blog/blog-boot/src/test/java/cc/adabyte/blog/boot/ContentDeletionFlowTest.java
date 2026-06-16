package cc.adabyte.blog.boot;

import cc.adabyte.blog.common.constants.ReviewStatus;
import cc.adabyte.blog.common.exception.BusinessException;
import cc.adabyte.blog.system.review.entity.ReviewTask;
import cc.adabyte.blog.system.review.mapper.ReviewTaskMapper;
import cc.adabyte.blog.system.review.service.ReviewService;
import cc.adabyte.blog.zoom.article.entity.Article;
import cc.adabyte.blog.zoom.article.entity.ArticleTag;
import cc.adabyte.blog.zoom.article.mapper.ArticleMapper;
import cc.adabyte.blog.zoom.article.mapper.ArticleTagMapper;
import cc.adabyte.blog.zoom.article.service.ArticleService;
import cc.adabyte.blog.zoom.message.entity.Message;
import cc.adabyte.blog.zoom.message.mapper.MessageMapper;
import cc.adabyte.blog.zoom.message.service.MessageService;
import cc.adabyte.blog.zoom.shared.enums.ArticleSource;
import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class ContentDeletionFlowTest {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ReviewTaskMapper reviewTaskMapper;

    @Test
    void deleteArticleRemovesPendingReviewTaskAndTags() {
        Long articleId = createArticle(ContentStatus.PENDING_REVIEW);
        createArticleTag(articleId);
        Long taskId = createReviewTask("article", articleId, ReviewStatus.PENDING);

        articleService.delete(articleId);

        assertThat(articleMapper.selectById(articleId)).isNull();
        assertThat(articleTagMapper.selectByArticleId(articleId)).isEmpty();
        assertThat(reviewTaskMapper.selectById(taskId)).isNull();
    }

    @Test
    void deleteArticleKeepsHandledReviewTask() {
        Long articleId = createArticle(ContentStatus.PUBLISHED);
        Long taskId = createReviewTask("article", articleId, ReviewStatus.APPROVED);

        articleService.delete(articleId);

        assertThat(articleMapper.selectById(articleId)).isNull();
        assertThat(reviewTaskMapper.selectById(taskId)).isNotNull();
    }

    @Test
    void deleteMessageRemovesPendingReviewTask() {
        Long messageId = createMessage(ContentStatus.PENDING_REVIEW);
        Long taskId = createReviewTask("message", messageId, ReviewStatus.PENDING);

        messageService.delete(messageId);

        assertThat(messageMapper.selectById(messageId)).isNull();
        assertThat(reviewTaskMapper.selectById(taskId)).isNull();
    }

    @Test
    void ignorePendingTaskRemovesUnderlyingMessage() {
        Long messageId = createMessage(ContentStatus.PENDING_REVIEW);
        Long taskId = createReviewTask("message", messageId, ReviewStatus.PENDING);

        reviewService.ignorePending(taskId);

        assertThat(reviewTaskMapper.selectById(taskId)).isNull();
        assertThat(messageMapper.selectById(messageId)).isNull();
    }

    @Test
    void ignoreHandledTaskIsRejected() {
        Long articleId = createArticle(ContentStatus.PUBLISHED);
        Long taskId = createReviewTask("article", articleId, ReviewStatus.APPROVED);

        assertThatThrownBy(() -> reviewService.ignorePending(taskId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅待审核任务支持忽略删除");

        assertThat(reviewTaskMapper.selectById(taskId)).isNotNull();
        assertThat(articleMapper.selectById(articleId)).isNotNull();
    }

    private Long createArticle(ContentStatus status) {
        Article article = new Article();
        article.setTitle("Delete Flow " + System.nanoTime());
        article.setSummary("summary");
        article.setContent("# body");
        article.setStatus(status);
        article.setSource(ArticleSource.ORIGINAL);
        article.setTop(false);
        article.setHidden(false);
        article.setViewCount(0);
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        articleMapper.insert(article);
        return article.getId();
    }

    private void createArticleTag(Long articleId) {
        ArticleTag articleTag = new ArticleTag();
        articleTag.setArticleId(articleId);
        articleTag.setTagId(1L);
        articleTag.setPrimary(true);
        articleTag.setSortOrder(0);
        articleTag.setCreateTime(LocalDateTime.now());
        articleTagMapper.insert(articleTag);
    }

    private Long createMessage(ContentStatus status) {
        Message message = new Message();
        message.setNickname("visitor");
        message.setEmail("visitor@example.com");
        message.setContent("hello");
        message.setStatus(status);
        message.setLikeCount(0);
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        messageMapper.insert(message);
        return message.getId();
    }

    private Long createReviewTask(String contentType, Long contentId, ReviewStatus status) {
        ReviewTask task = new ReviewTask();
        task.setContentType(contentType);
        task.setContentId(contentId);
        task.setStatus(status);
        task.setSubmitterType("test");
        task.setSubmitterId("tester");
        task.setSubmitTime(LocalDateTime.now());
        if (status != ReviewStatus.PENDING) {
            task.setReviewTime(LocalDateTime.now());
        }
        reviewTaskMapper.insert(task);
        return task.getId();
    }
}
