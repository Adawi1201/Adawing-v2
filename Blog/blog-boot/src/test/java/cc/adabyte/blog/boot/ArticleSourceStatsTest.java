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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /articles/stats 来源计数回归测试。
 *
 * <p>约定：agentCount 与列表标签共用同一口径——source_agent 非空即 Agent 创作；
 * 即使 source 列与 source_agent 不一致（历史数据），计数也必须跟随展示规则。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DisplayName("文章 stats 来源计数口径测试")
class ArticleSourceStatsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final List<Long> seededIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        seededIds.forEach(articleMapper::deleteById);
        seededIds.clear();
    }

    private Article insertArticle(ArticleSource source, String sourceAgent) {
        Article a = new Article();
        a.setTitle("stats 口径锚点 " + System.nanoTime());
        a.setSummary("摘要");
        a.setContent("正文");
        a.setStatus(ContentStatus.PUBLISHED);
        a.setSource(source);
        a.setSourceAgent(sourceAgent);
        a.setTop(false);
        a.setHidden(false);
        a.setViewCount(0);
        a.setCreateTime(LocalDateTime.now());
        a.setUpdateTime(LocalDateTime.now());
        articleMapper.insert(a);
        seededIds.add(a.getId());
        return a;
    }

    private String adminToken() {
        String username = "source-stats-admin";
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

    private JsonNode statsData() throws Exception {
        String body = mockMvc.perform(get("/api/v2/articles/stats")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data");
    }

    @Test
    @DisplayName("agentCount 按 source_agent 非空计数，original + agent = total")
    void agentCountFollowsSourceAgentPresence() throws Exception {
        JsonNode before = statsData();
        long agentBefore = before.get("agentCount").asLong();
        long originalBefore = before.get("originalCount").asLong();
        long totalBefore = before.get("total").asLong();

        insertArticle(ArticleSource.ORIGINAL, null);
        insertArticle(ArticleSource.AI_GENERATED, "opencode");
        // source 列与 source_agent 不一致的异常数据：计数跟随 source_agent
        insertArticle(ArticleSource.ORIGINAL, "codex");

        JsonNode after = statsData();
        assertEquals(agentBefore + 2, after.get("agentCount").asLong(), "sourceAgent 非空的文章都应计入 agentCount");
        assertEquals(originalBefore + 1, after.get("originalCount").asLong(), "originalCount = total - agentCount");
        assertEquals(totalBefore + 3, after.get("total").asLong());
    }
}
