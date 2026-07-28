package cc.adabyte.blog.zoom.article.controller;

import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.common.result.Result;
import cc.adabyte.blog.zoom.article.dto.TagWithCount;
import cc.adabyte.blog.zoom.article.entity.Article;
import cc.adabyte.blog.zoom.article.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文章-标签关系端点。
 *
 * <p>承载需要访问 {@code article_tag} 的操作（按标签查文章、标签合并/删除、带引用数的标签列表）。
 * 纯标签词表操作（创建/建议/列表）仍在 {@code zoom-tag} 的 TagController。
 */
@RestController
@RequestMapping("/api/v2/article-tags")
@RequiredArgsConstructor
public class ArticleTagController {

    private final ArticleService articleService;

    /** 访客：某标签下的已发布文章（分页）。 */
    @GetMapping("/by-tag")
    public Result<PageResult<Article>> listByTag(
            @RequestParam String tag,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(articleService.listByTag(tag, page, size));
    }

    /** 管理端：标签及其文章引用数。 */
    @GetMapping
    public Result<List<TagWithCount>> listWithCount() {
        return Result.ok(articleService.listTagsWithCount());
    }

    /** 管理端：合并标签（源并入目标，源删除）。 */
    @PostMapping("/{sourceId}/merge/{targetId}")
    public Result<Void> merge(@PathVariable Long sourceId, @PathVariable Long targetId) {
        articleService.mergeTags(sourceId, targetId);
        return Result.ok();
    }

    /** 管理端：删除未被引用的标签。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        articleService.deleteTag(id);
        return Result.ok();
    }
}
