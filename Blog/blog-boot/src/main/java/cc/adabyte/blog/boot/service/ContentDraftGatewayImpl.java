package cc.adabyte.blog.boot.service;

import cc.adabyte.blog.common.gateway.ContentDraftGateway;
import cc.adabyte.blog.common.model.SubmitReviewRequest;
import cc.adabyte.blog.system.review.service.ReviewService;
import cc.adabyte.blog.zoom.article.entity.Article;
import cc.adabyte.blog.zoom.article.service.ArticleService;
import cc.adabyte.blog.zoom.shared.enums.ArticleSource;
import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentDraftGatewayImpl implements ContentDraftGateway {

    private final ArticleService articleService;
    private final ReviewService reviewService;

    @Override
    public Long createArticleDraft(String title, String content, String summary, String sourceAgent) {
        Article article = new Article();
        article.setTitle(title);
        article.setContent(content);
        article.setSummary(summary);
        article.setSource(ArticleSource.AI_GENERATED);
        article.setSourceAgent(sourceAgent);
        article.setStatus(ContentStatus.DRAFT);
        article.setTop(false);
        article.setHidden(false);
        article.setViewCount(0);

        articleService.saveOrUpdate(article, null);
        log.info("[ContentDraftGateway] Created draft id={} title={}", article.getId(), title);

        // 自动提交审核链：1) 创建 review_task 记录  2) 更新 article status 为 PENDING_REVIEW
        SubmitReviewRequest reviewReq = new SubmitReviewRequest();
        reviewReq.setContentType("article");
        reviewReq.setContentId(article.getId());
        reviewReq.setSubmitterType("agent");
        reviewReq.setSubmitterId(sourceAgent);
        reviewService.submit(reviewReq);
        log.info("[ContentDraftGateway] Created review task: articleId={}", article.getId());

        articleService.submitForReview(article.getId());
        log.info("[ContentDraftGateway] Auto-submitted for review: articleId={}", article.getId());

        return article.getId();
    }
}
