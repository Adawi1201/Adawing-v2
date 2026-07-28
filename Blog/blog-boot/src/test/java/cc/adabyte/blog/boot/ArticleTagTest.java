package cc.adabyte.blog.boot;

import cc.adabyte.blog.common.exception.BusinessException;
import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.zoom.article.dto.TagWithCount;
import cc.adabyte.blog.zoom.article.entity.Article;
import cc.adabyte.blog.zoom.article.mapper.ArticleTagMapper;
import cc.adabyte.blog.zoom.article.service.ArticleService;
import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import cc.adabyte.blog.zoom.tag.entity.Tag;
import cc.adabyte.blog.zoom.tag.mapper.TagMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文章标签落地 / 按标签查询 / 合并 / 删除 / 计数 回归测试。
 * 覆盖 Spec AC1/AC3/AC4/AC5/AC6。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DisplayName("文章标签重构测试")
class ArticleTagTest {

    @Autowired
    private ArticleService articleService;
    @Autowired
    private TagMapper tagMapper;
    @Autowired
    private ArticleTagMapper articleTagMapper;

    private Article newArticle(String title) {
        Article a = new Article();
        a.setTitle(title);
        a.setContent("正文内容");
        a.setStatus(ContentStatus.PUBLISHED);
        return a;
    }

    @Test
    @DisplayName("AC1 — 保存文章落地标签，详情回显 tags，article_tag 有数据")
    void saveBindsTags() {
        Article a = newArticle("标签测试文章");
        articleService.saveOrUpdate(a, List.of("Java", "Spring"));
        assertNotNull(a.getId());

        Article loaded = articleService.getAdminById(a.getId());
        assertNotNull(loaded.getTags());
        assertEquals(2, loaded.getTags().size(), "应回显 2 个标签");
        assertTrue(loaded.getTags().stream().anyMatch(t -> t.name().equals("Java")));
    }

    @Test
    @DisplayName("AC1 — 重复标签名复用词条，不新增 tag 行")
    void reuseExistingTag() {
        Article a1 = newArticle("文章A");
        articleService.saveOrUpdate(a1, List.of("Kotlin"));
        Tag first = tagMapper.selectByName("Kotlin");
        assertNotNull(first);

        Article a2 = newArticle("文章B");
        articleService.saveOrUpdate(a2, List.of("Kotlin"));
        Tag second = tagMapper.selectByName("Kotlin");
        assertEquals(first.getId(), second.getId(), "同名标签应复用同一词条");
    }

    @Test
    @DisplayName("AC3 — 按标签查询已发布文章")
    void listByTag() {
        Article a = newArticle("可检索文章");
        articleService.saveOrUpdate(a, List.of("检索专用标签"));
        articleService.publish(a.getId());

        PageResult<Article> result = articleService.listByTag("检索专用标签", 1, 10);
        assertFalse(result.getList().isEmpty(), "应能按标签查到文章");
        assertTrue(result.getList().stream().anyMatch(x -> x.getId().equals(a.getId())));
        // 查到的文章应带回标签
        Article found = result.getList().stream().filter(x -> x.getId().equals(a.getId())).findFirst().orElseThrow();
        assertNotNull(found.getTags());
        assertTrue(found.getTags().stream().anyMatch(t -> t.name().equals("检索专用标签")));
    }

    @Test
    @DisplayName("AC4 — 标签列表含真实引用数")
    void tagCount() {
        Article a = newArticle("计数文章");
        articleService.saveOrUpdate(a, List.of("计数标签"));

        List<TagWithCount> list = articleService.listTagsWithCount();
        TagWithCount tc = list.stream().filter(t -> t.name().equals("计数标签")).findFirst().orElseThrow();
        assertEquals(1, tc.articleCount());
    }

    @Test
    @DisplayName("AC5 — 合并标签：源关联迁移到目标并去重，源标签删除")
    void mergeTags() {
        Article a = newArticle("合并文章");
        articleService.saveOrUpdate(a, List.of("旧标签", "目标标签"));
        Tag source = tagMapper.selectByName("旧标签");
        Tag target = tagMapper.selectByName("目标标签");

        articleService.mergeTags(source.getId(), target.getId());

        assertNull(tagMapper.selectById(source.getId()), "源标签应被删除");
        // 文章原同时挂有源+目标，合并后只保留目标一条（冲突去重）
        assertEquals(0, articleTagMapper.countByTagId(source.getId()));
        assertEquals(1, articleTagMapper.countByTagId(target.getId()));
    }

    @Test
    @DisplayName("AC6 — 被引用标签不可删除，未引用可删除")
    void deleteTagGuard() {
        Article a = newArticle("删除守卫文章");
        articleService.saveOrUpdate(a, List.of("在用标签"));
        Tag used = tagMapper.selectByName("在用标签");

        assertThrows(BusinessException.class, () -> articleService.deleteTag(used.getId()), "在用标签不可删除");

        // 合并到别处后源变为未引用，可删
        Article b = newArticle("另一篇");
        articleService.saveOrUpdate(b, List.of("空闲标签"));
        Tag idle = tagMapper.selectByName("空闲标签");
        articleTagMapper.deleteByArticleId(b.getId());
        assertDoesNotThrow(() -> articleService.deleteTag(idle.getId()));
        assertNull(tagMapper.selectById(idle.getId()));
    }
}
