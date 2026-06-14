package cc.adabyte.blog.zoom.article.controller;

import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.common.result.Result;
import cc.adabyte.blog.resource.core.service.ResourceAllocationFacade;
import cc.adabyte.blog.zoom.article.dto.ArticleSaveRequest;
import cc.adabyte.blog.zoom.article.entity.Article;
import cc.adabyte.blog.zoom.article.service.ArticleService;
import cc.adabyte.blog.zoom.shared.enums.ArticleSource;
import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final ResourceAllocationFacade resourceFacade;

    @GetMapping("/published")
    public Result<PageResult<Article>> listPublished(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(articleService.listPublished(page, size));
    }

    @GetMapping("/admin")
    public Result<PageResult<Article>> listAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(articleService.listAll(page, size));
    }

    @GetMapping("/admin/{id}")
    public Result<Article> getAdminById(@PathVariable Long id) {
        Article article = articleService.getAdminById(id);
        if (article != null) {
            article.setContent(resourceFacade.renderMarkdown(article.getContent()));
        }
        return Result.ok(article);
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", articleService.countByStatus(null));
        stats.put("draftCount", articleService.countByStatus(ContentStatus.DRAFT));
        stats.put("pendingCount", articleService.countByStatus(ContentStatus.PENDING_REVIEW));
        stats.put("publishedCount", articleService.countByStatus(ContentStatus.PUBLISHED));
        stats.put("rejectedCount", articleService.countByStatus(ContentStatus.REJECTED));
        stats.put("hiddenCount", articleService.countByStatus(ContentStatus.HIDDEN));
        stats.put("originalCount", articleService.countBySource(ArticleSource.ORIGINAL));
        stats.put("agentCount", articleService.countBySource(ArticleSource.AI_GENERATED));
        stats.put("totalViewCount", articleService.getTotalViewCount());
        return Result.ok(stats);
    }

    @GetMapping("/{id}")
    public Result<Article> getPublishedById(@PathVariable Long id) {
        Article article = articleService.getPublishedById(id);
        if (article != null) {
            article.setContent(resourceFacade.renderMarkdown(article.getContent()));
        }
        return Result.ok(article);
    }

    @GetMapping("/archive")
    public Result<Map<String, List<Article>>> listArchive() {
        return Result.ok(articleService.listArchive());
    }

    @PostMapping
    public Result<Void> saveOrUpdate(@Valid @RequestBody ArticleSaveRequest request) {
        Article article = new Article();
        article.setId(request.getId());
        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setCoverResourceId(request.getCoverResourceId());
        articleService.saveOrUpdate(article, request.getTagNames());
        return Result.ok();
    }

    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        articleService.publish(id);
        return Result.ok();
    }

    @PostMapping("/{id}/review")
    public Result<Void> submitForReview(@PathVariable Long id) {
        articleService.submitForReview(id);
        return Result.ok();
    }

    @PostMapping("/{id}/hide")
    public Result<Void> hide(@PathVariable Long id) {
        articleService.hide(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteDraft(@PathVariable Long id) {
        articleService.deleteDraft(id);
        return Result.ok();
    }
}
