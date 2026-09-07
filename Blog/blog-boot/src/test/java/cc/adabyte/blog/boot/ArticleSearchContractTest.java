package cc.adabyte.blog.boot;

import cc.adabyte.blog.common.util.JwtUtil;
import cc.adabyte.blog.system.auth.entity.SysUser;
import cc.adabyte.blog.system.auth.enums.UserRole;
import cc.adabyte.blog.system.auth.enums.UserStatus;
import cc.adabyte.blog.system.auth.mapper.SysUserMapper;
import cc.adabyte.blog.zoom.article.entity.Article;
import cc.adabyte.blog.zoom.article.mapper.ArticleMapper;
import cc.adabyte.blog.zoom.shared.enums.ArticleSource;
import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文章公开搜索契约 + hide/unhide 回归测试。
 *
 * <p>约定：{@code GET /api/v2/articles/search} 对访客匿名开放，仅命中已发布且未隐藏文章；
 * {@code POST /api/v2/articles/{id}/unhide} 为管理端端点，隐藏文章经其恢复可见。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DisplayName("文章公开搜索与 hide/unhide 测试")
class ArticleSearchContractTest {

    private static final String KEYWORD = "搜索契约锚点7f3a";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final List<Long> seededIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        seededIds.forEach(articleMapper::deleteById);
        seededIds.clear();
    }

    private Article insertArticle(String title, ContentStatus status, boolean hidden) {
        Article a = new Article();
        a.setTitle(title);
        a.setSummary("摘要 " + KEYWORD);
        a.setContent("正文");
        a.setStatus(status);
        a.setSource(ArticleSource.ORIGINAL);
        a.setTop(false);
        a.setHidden(hidden);
        a.setViewCount(0);
        a.setCreateTime(LocalDateTime.now());
        a.setUpdateTime(LocalDateTime.now());
        articleMapper.insert(a);
        seededIds.add(a.getId());
        return a;
    }

    private String adminToken() {
        String username = "search-contract-admin";
        SysUser existing = sysUserMapper.selectByUsername(username);
        if (existing == null) {
            SysUser u = new SysUser();
            u.setUsername(username);
            u.setPasswordHash("unused");
            u.setStatus(UserStatus.ACTIVE);
            u.setRole(UserRole.ADMIN);
            sysUserMapper.insert(u);
        }
        return JwtUtil.generateToken(username, jwtSecret);
    }

    private String searchBody(String keyword) throws Exception {
        return mockMvc.perform(get("/api/v2/articles/search").param("keyword", keyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
    }

    private static String idFragment(Article a) {
        return "\"id\":" + a.getId();
    }

    @Test
    @DisplayName("匿名搜索仅命中已发布文章，草稿与隐藏文章不可见")
    void anonymousSearchFiltersUnpublished() throws Exception {
        Article published = insertArticle("已发布 " + KEYWORD, ContentStatus.PUBLISHED, false);
        Article draft = insertArticle("草稿 " + KEYWORD, ContentStatus.DRAFT, false);
        Article hidden = insertArticle("已隐藏 " + KEYWORD, ContentStatus.PUBLISHED, true);

        String body = searchBody(KEYWORD);

        assertTrue(body.contains(idFragment(published)), "已发布文章应被搜出");
        assertFalse(body.contains(idFragment(draft)), "草稿不应被搜出");
        assertFalse(body.contains(idFragment(hidden)), "隐藏文章不应被搜出");
    }

    @Test
    @DisplayName("空关键词返回空列表")
    void blankKeywordReturnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v2/articles/search").param("keyword", "  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("hide 后搜不到，unhide 后恢复可见")
    void hideThenUnhide() throws Exception {
        Article article = insertArticle("可见性 " + KEYWORD, ContentStatus.PUBLISHED, false);
        String token = adminToken();

        mockMvc.perform(post("/api/v2/articles/" + article.getId() + "/hide")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertFalse(searchBody(KEYWORD).contains(idFragment(article)), "hide 后不应被搜出");

        String adminBody = mockMvc.perform(get("/api/v2/articles/admin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(adminBody.contains("\"id\":" + article.getId()),
                "管理端列表应仍包含被隐藏文章");
        assertTrue(adminBody.contains("\"hidden\":true"),
                "管理端列表应能读到 is_hidden 标记");

        mockMvc.perform(post("/api/v2/articles/" + article.getId() + "/unhide")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertTrue(searchBody(KEYWORD).contains(idFragment(article)), "unhide 后应恢复可见");
    }

    @Test
    @DisplayName("匿名调用 unhide 返回 401")
    void anonymousUnhideUnauthorized() throws Exception {
        Article article = insertArticle("鉴权 " + KEYWORD, ContentStatus.PUBLISHED, true);

        mockMvc.perform(post("/api/v2/articles/" + article.getId() + "/unhide"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }
}
