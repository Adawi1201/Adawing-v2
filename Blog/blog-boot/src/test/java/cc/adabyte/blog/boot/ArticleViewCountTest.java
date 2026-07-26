package cc.adabyte.blog.boot;

import cc.adabyte.blog.zoom.article.entity.Article;
import cc.adabyte.blog.zoom.article.mapper.ArticleMapper;
import cc.adabyte.blog.zoom.article.service.ArticleService;
import cc.adabyte.blog.zoom.article.service.ViewCountBuffer;
import cc.adabyte.blog.zoom.shared.enums.ArticleSource;
import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文章浏览量缓存刷库回归测试。
 * 覆盖 Spec AC1-AC3：访问内存累加（DB 不变）、返回值含增量、sync 后落库、未命中不累加。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DisplayName("文章浏览量缓存刷库测试")
class ArticleViewCountTest {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ViewCountBuffer viewCountBuffer;

    @BeforeEach
    void clearBuffer() {
        // 清空共享缓冲器，隔离用例间的残余增量
        viewCountBuffer.drain();
    }

    private Article insertArticle(ContentStatus status, boolean hidden) {
        Article a = new Article();
        a.setTitle("浏览量测试文章");
        a.setSummary("摘要");
        a.setContent("正文");
        a.setStatus(status);
        a.setSource(ArticleSource.ORIGINAL);
        a.setTop(false);
        a.setHidden(hidden);
        a.setViewCount(0);
        a.setCreateTime(LocalDateTime.now());
        a.setUpdateTime(LocalDateTime.now());
        articleMapper.insert(a);
        return a;
    }

    @Test
    @DisplayName("AC1/AC3 — 访问只累加内存，返回值含增量，DB 在 sync 前不变")
    void accumulatesInMemory() {
        Article seed = insertArticle(ContentStatus.PUBLISHED, false);

        Article first = articleService.getPublishedById(seed.getId());
        assertNotNull(first, "已发布文章应可访问");
        assertEquals(1, first.getViewCount(), "返回值应含内存增量(0→1)");

        Article second = articleService.getPublishedById(seed.getId());
        assertEquals(2, second.getViewCount(), "第二次访问返回 2");

        // sync 前 DB 仍为 0
        assertEquals(0, articleMapper.selectAdminById(seed.getId()).getViewCount(), "sync 前 DB 应仍为 0");
        assertEquals(2, viewCountBuffer.peek(seed.getId()), "内存增量应为 2");
    }

    @Test
    @DisplayName("AC2 — sync 后增量落库，缓冲清零")
    void syncFlushesToDb() {
        Article seed = insertArticle(ContentStatus.PUBLISHED, false);

        articleService.getPublishedById(seed.getId());
        articleService.getPublishedById(seed.getId());
        articleService.getPublishedById(seed.getId());

        articleService.syncViewCountsToDb();

        assertEquals(3, articleMapper.selectAdminById(seed.getId()).getViewCount(), "sync 后 DB 应为 3");
        assertEquals(0, viewCountBuffer.peek(seed.getId()), "sync 后内存增量应清零");
    }

    @Test
    @DisplayName("AC2 — 访问不存在文章返回 null 且不累加")
    void nonExistentReturnsNull() {
        assertNull(articleService.getPublishedById(999_999_999L));
        assertEquals(0, viewCountBuffer.peek(999_999_999L), "不存在文章不应累加");
    }

    @Test
    @DisplayName("AC2 — 草稿不可访问且不累加")
    void draftNotAccessibleNoIncrement() {
        Article draft = insertArticle(ContentStatus.DRAFT, false);

        assertNull(articleService.getPublishedById(draft.getId()), "草稿不应通过发布接口访问");
        assertEquals(0, viewCountBuffer.peek(draft.getId()), "草稿不应累加");

        articleService.syncViewCountsToDb();
        assertEquals(0, articleMapper.selectAdminById(draft.getId()).getViewCount(), "草稿 DB view_count 应保持 0");
    }

    @Test
    @DisplayName("AC2 — 隐藏文章不可访问且不累加")
    void hiddenNotAccessibleNoIncrement() {
        Article hidden = insertArticle(ContentStatus.PUBLISHED, true);

        assertNull(articleService.getPublishedById(hidden.getId()), "隐藏文章不应通过发布接口访问");
        assertEquals(0, viewCountBuffer.peek(hidden.getId()), "隐藏文章不应累加");

        articleService.syncViewCountsToDb();
        assertEquals(0, articleMapper.selectAdminById(hidden.getId()).getViewCount(), "隐藏文章 DB view_count 应保持 0");
    }
}
