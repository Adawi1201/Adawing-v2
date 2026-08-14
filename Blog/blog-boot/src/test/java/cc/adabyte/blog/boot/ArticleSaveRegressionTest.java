package cc.adabyte.blog.boot;

import cc.adabyte.blog.zoom.article.entity.Article;
import cc.adabyte.blog.zoom.article.service.ArticleService;
import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 编辑器保存回归：更新路径只携带标题/摘要/正文/封面（status/viewCount 为 null），
 * 不得把已发布文章打回草稿、不得清空阅读量。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DisplayName("编辑器保存保留状态回归测试")
class ArticleSaveRegressionTest {

    @Autowired
    private ArticleService articleService;

    @Test
    @DisplayName("更新已发布文章：阅读量与发布状态保持不变")
    void updatePreservesViewCountAndStatus() {
        Article a = new Article();
        a.setTitle("已发布文章");
        a.setContent("原始正文");
        a.setStatus(ContentStatus.PUBLISHED);
        a.setViewCount(42);
        articleService.saveOrUpdate(a, List.of());
        assertNotNull(a.getId());

        // 模拟编辑器保存：只回传 id/标题/摘要/正文/封面
        Article edit = new Article();
        edit.setId(a.getId());
        edit.setTitle("已发布文章（改）");
        edit.setSummary("新摘要");
        edit.setContent("修改后的正文");
        articleService.saveOrUpdate(edit, List.of());

        Article reloaded = articleService.getAdminById(a.getId());
        assertEquals(Integer.valueOf(42), reloaded.getViewCount(), "编辑保存不得清空阅读量");
        assertEquals(ContentStatus.PUBLISHED, reloaded.getStatus(), "编辑保存不得打回草稿");
        assertEquals("修改后的正文", reloaded.getContent());
    }
}
