package cc.adabyte.blog.boot;

import cc.adabyte.blog.common.result.PageResult;
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
 * 分页拦截器契约测试。
 *
 * <p>约定：Mapper 中的 {@code Page} 参数须真正生效——查询行数被 {@code LIMIT}
 * 截断到 size，且 {@code total} 反映真实总数。通过创建多于单页容量的文章验证：
 * <ul>
 *   <li>返回行数为 size（而非全部）；</li>
 *   <li>{@code total} 为真实总数（而非 0）。</li>
 * </ul>
 * 两条断言各自独立。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DisplayName("分页拦截器回归测试")
class PaginationInterceptorTest {

    @Autowired
    private ArticleService articleService;

    private static final int PAGE_SIZE = 5;
    private static final int CREATE_COUNT = 12;

    @Test
    @DisplayName("Page 参数生效：结果被 LIMIT 截断，total 反映真实总数")
    void paginationLimitsAndCounts() {
        for (int i = 0; i < CREATE_COUNT; i++) {
            Article a = new Article();
            a.setTitle("分页文章-" + i);
            a.setContent("正文");
            a.setStatus(ContentStatus.PUBLISHED);
            articleService.saveOrUpdate(a, List.of());
        }

        PageResult<Article> firstPage = articleService.listPublished(1, PAGE_SIZE);

        assertEquals(PAGE_SIZE, firstPage.getList().size(),
                "拦截器生效时单页应被 LIMIT 截断到 size；若返回全部行说明拦截器未注册");
        assertTrue(firstPage.getTotal() >= CREATE_COUNT,
                "total 应反映真实总数；恒为 0 说明拦截器未注册");
        assertTrue(firstPage.getTotal() > firstPage.getList().size(),
                "总数大于单页行数，证明分页确实分页而非全量返回");
    }
}
