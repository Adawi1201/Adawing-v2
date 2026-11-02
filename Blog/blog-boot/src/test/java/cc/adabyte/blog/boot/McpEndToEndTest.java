package cc.adabyte.blog.boot;

import cc.adabyte.blog.common.gateway.McpKeyValidator;
import cc.adabyte.blog.system.config.service.SystemConfigService;
import cc.adabyte.blog.zoom.article.dto.ArticleTagRow;
import cc.adabyte.blog.zoom.article.entity.Article;
import cc.adabyte.blog.zoom.article.mapper.ArticleTagMapper;
import cc.adabyte.blog.zoom.article.service.ArticleService;
import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import cc.adabyte.blog.zoom.tag.entity.Tag;
import cc.adabyte.blog.zoom.tag.mapper.TagMapper;
import cc.adabyte.blog.zoom.tag.service.TagService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Agent-MCP 端到端集成测试。
 * 覆盖完整链路：POST /mcp → initialize → ping → tools/list → resources/list|read → Tool 调用。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("MCP 端到端集成测试")
class McpEndToEndTest {

    private static final String SESSION_HEADER = "Mcp-Session-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Autowired
    private TagService tagService;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private SystemConfigService systemConfigService;

    private String sessionId;

    /** Stub — 接受任意 API Key 以绕过认证 */
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public McpKeyValidator stubKeyValidator() {
            return key -> true;
        }
    }

    @BeforeEach
    void initSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-MCP-Key", "test-key-any-value")
                        .content(rpc("initialize", Map.of(
                                "protocolVersion", "2025-03-26",
                                "clientInfo", Map.of("name", "test-client", "version", "1.0")))))
                .andExpect(status().isOk())
                .andReturn();
        sessionId = result.getResponse().getHeader(SESSION_HEADER);
        assertNotNull(sessionId, "initialize 应返回会话 Header");
    }

    // ---- helpers ----

    private String rpc(String method, Object params) throws Exception {
        ObjectNode body = objectMapper.createObjectNode()
                .put("jsonrpc", "2.0")
                .put("id", 1)
                .put("method", method);
        if (params != null) {
            body.set("params", objectMapper.valueToTree(params));
        }
        return objectMapper.writeValueAsString(body);
    }

    private MvcResult mcpCall(String method, Object params) throws Exception {
        return mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-MCP-Key", "test-key-any-value")
                        .header(SESSION_HEADER, sessionId)
                        .content(rpc(method, params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andReturn();
    }

    private JsonNode parseResult(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("result");
    }

    private void expectRpcError(String method, Object params, int code) throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-MCP-Key", "test-key")
                        .header(SESSION_HEADER, sessionId)
                        .content(rpc(method, params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(code));
    }

    // ---- tests ----

    @Test
    @Order(1)
    @DisplayName("initialize — 协商协议版本并只声明 tools 能力")
    void initialize() throws Exception {
        MvcResult result = mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-MCP-Key", "test-key-any-value")
                        .content(rpc("initialize", Map.of(
                                "protocolVersion", "2025-06-18",
                                "clientInfo", Map.of("name", "opencode-test")))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode r = parseResult(result);

        assertEquals("2025-06-18", r.get("protocolVersion").asText(), "应回显客户端请求的受支持版本");
        assertEquals("adawing-mcp-server", r.get("serverInfo").get("name").asText());
        assertTrue(r.get("capabilities").has("tools"));
        assertFalse(r.get("capabilities").has("resources"), "不再提供 resources 能力");
    }

    @Test
    @Order(2)
    @DisplayName("initialize — 未知协议版本回退到默认版本")
    void initializeFallbackVersion() throws Exception {
        MvcResult result = mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-MCP-Key", "test-key-any-value")
                        .content(rpc("initialize", Map.of(
                                "protocolVersion", "1999-01-01",
                                "clientInfo", Map.of("name", "legacy-client")))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode r = parseResult(result);
        assertEquals("2025-03-26", r.get("protocolVersion").asText());
    }

    @Test
    @Order(3)
    @DisplayName("ping — 返回空结果")
    void ping() throws Exception {
        MvcResult result = mcpCall("ping", null);
        assertNotNull(parseResult(result));
    }

    @Test
    @Order(4)
    @DisplayName("resources/list、resources/read、prompts/list — 未提供的能力返回 -32601")
    void undeclaredCapabilitiesNotFound() throws Exception {
        expectRpcError("resources/list", null, -32601);
        expectRpcError("resources/read", Map.of("uri", "adawing://content-rules"), -32601);
        expectRpcError("prompts/list", null, -32601);
    }

    @Test
    @Order(6)
    @DisplayName("JSON-RPC 响应序列化 — null 的 result/error 字段不输出（MCP SDK Zod 严格校验要求）")
    void responseOmitsNullFields() throws Exception {
        MvcResult okResult = mcpCall("ping", null);
        String okBody = okResult.getResponse().getContentAsString();
        assertFalse(okBody.contains("\"error\""), "成功响应不应包含 error 字段: " + okBody);

        MvcResult errResult = mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-MCP-Key", "test-key")
                        .header(SESSION_HEADER, sessionId)
                        .content(rpc("nonexistent_method", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32601))
                .andReturn();
        String errBody = errResult.getResponse().getContentAsString();
        assertFalse(errBody.contains("\"result\""), "错误响应不应包含 result 字段: " + errBody);
    }

    @Test
    @Order(8)
    @DisplayName("tools/list — 不再注册 rules 与 note 查询类 Tool")
    void toolsList() throws Exception {
        MvcResult result = mcpCall("tools/list", null);
        JsonNode r = parseResult(result);

        assertTrue(r.has("tools"));
        var names = r.get("tools").findValuesAsText("name");
        assertTrue(names.containsAll(java.util.List.of(
                "create_article_draft", "create_note_draft", "search_articles", "get_article")), "" + names);
        for (String removed : java.util.List.of("get_content_rules", "get_note_rules", "search_notes", "get_note")) {
            assertFalse(names.contains(removed), "已移除的 Tool 不应注册: " + removed);
        }
    }

    @Test
    @Order(9)
    @DisplayName("tools/call sourceAgent 枚举 — create 工具的 schema 覆盖全部已支持的 agent")
    void sourceAgentEnum() throws Exception {
        JsonNode tools = parseResult(mcpCall("tools/list", null)).get("tools");
        for (JsonNode tool : tools) {
            String name = tool.get("name").asText();
            if (!name.startsWith("create_")) continue;
            java.util.List<String> agents = new java.util.ArrayList<>();
            tool.get("inputSchema").get("properties").get("sourceAgent").get("enum").forEach(n -> agents.add(n.asText()));
            assertTrue(agents.containsAll(java.util.List.of("codex", "claude-code", "openclaw", "opencode")),
                    name + " sourceAgent 枚举不完整: " + agents);
        }
    }

    private static Long createdDraftId;

    @Test
    @Order(10)
    @DisplayName("tools/call create_article_draft — 创建草稿并返回 ID")
    void createArticleDraft() throws Exception {
        MvcResult result = mcpCall("tools/call", Map.of(
                "name", "create_article_draft",
                "arguments", Map.of(
                        "title", "端到端测试文章",
                        "content", "# 测试\n这是 AI 生成的测试文章内容。",
                        "summary", "测试摘要",
                        "sourceAgent", "test-agent"
                )
        ));
        JsonNode r = parseResult(result);

        assertTrue(r.has("content"));
        String text = r.get("content").get(0).get("text").asText();
        JsonNode data = objectMapper.readTree(text);

        assertTrue(data.has("draftId"), "应返回 draftId");
        createdDraftId = data.get("draftId").asLong();
        assertTrue(createdDraftId > 0, "draftId 应为正数");
    }

    private Long createPublishedArticle(String title) {
        Article article = new Article();
        article.setTitle(title);
        article.setContent("# 已发布\n正文");
        article.setSummary("已发布摘要");
        article.setStatus(ContentStatus.PUBLISHED);
        articleService.saveOrUpdate(article, null);
        return article.getId();
    }

    private JsonNode toolData(String name, Object arguments) throws Exception {
        MvcResult result = mcpCall("tools/call", Map.of("name", name, "arguments", arguments));
        return objectMapper.readTree(parseResult(result).get("content").get(0).get("text").asText());
    }

    @Test
    @Order(11)
    @DisplayName("tools/call get_article — 只返回已发布文章，待审草稿查不到")
    void getArticle() throws Exception {
        assertNotNull(createdDraftId, "依赖上一个测试创建的草稿 ID");

        JsonNode draft = toolData("get_article", Map.of("id", createdDraftId));
        assertTrue(draft.has("error"), "待审草稿不应被 get_article 返回: " + draft);

        Long publishedId = createPublishedArticle("端到端已发布文章");
        JsonNode data = toolData("get_article", Map.of("id", publishedId));
        assertFalse(data.has("error"), "不应返回错误: " + data);
        assertEquals(publishedId.longValue(), data.get("id").asLong());
        assertEquals("端到端已发布文章", data.get("title").asText());
        assertTrue(data.get("content").asText().contains("# 已发布"));
    }

    @Test
    @Order(12)
    @DisplayName("tools/call search_articles — 只命中已发布文章")
    void searchArticles() throws Exception {
        assertNotNull(createdDraftId, "依赖创建草稿测试");
        Long publishedId = createPublishedArticle("端到端测试已发布");

        JsonNode data = toolData("search_articles", Map.of("keyword", "端到端测试", "limit", 10));

        assertTrue(data.has("items"));
        var ids = data.get("items").findValuesAsText("id");
        assertTrue(ids.contains(String.valueOf(publishedId)), "应命中已发布文章: " + ids);
        assertFalse(ids.contains(String.valueOf(createdDraftId)), "不应命中待审草稿: " + ids);
    }

    // ---- tags ----

    private static final String EXISTING_TAG = "mcp-e2e-tag";
    private static final String UNKNOWN_TAG = "mcp-e2e-不存在的标签";

    private void ensureTag(String name) {
        if (tagMapper.selectByName(name) == null) {
            Tag tag = new Tag();
            tag.setName(name);
            tagService.create(tag);
        }
    }

    @Test
    @Order(30)
    @DisplayName("tools/call get_tags — 返回已有标签名")
    void getTags() throws Exception {
        ensureTag(EXISTING_TAG);
        JsonNode data = toolData("get_tags", Map.of());
        var names = new java.util.ArrayList<String>();
        data.get("tags").forEach(n -> names.add(n.asText()));
        assertTrue(names.contains(EXISTING_TAG), "应包含已有标签: " + names);
    }

    @Test
    @Order(31)
    @DisplayName("tools/call create_article_draft tags — 只绑定已有标签，未命中的放入 ignoredTags 且不新建")
    void createArticleDraftWithTags() throws Exception {
        ensureTag(EXISTING_TAG);
        JsonNode data = toolData("create_article_draft", Map.of(
                "title", "带标签的草稿",
                "content", "正文",
                "sourceAgent", "opencode",
                "tags", java.util.List.of(EXISTING_TAG, UNKNOWN_TAG)
        ));

        assertTrue(data.has("draftId"), "应返回 draftId: " + data);
        assertEquals(java.util.List.of(UNKNOWN_TAG), objectMapper.convertValue(data.get("ignoredTags"), java.util.List.class));

        Long draftId = data.get("draftId").asLong();
        var bound = articleTagMapper.selectTagViewsByArticleIds(java.util.List.of(draftId)).stream()
                .map(ArticleTagRow::name).toList();
        assertEquals(java.util.List.of(EXISTING_TAG), bound);
        assertNull(tagMapper.selectByName(UNKNOWN_TAG), "不应新建标签");
    }

    @Test
    @Order(32)
    @DisplayName("tools/call create_article_draft — 未传 tags 时 ignoredTags 为空数组")
    void createArticleDraftWithoutTags() throws Exception {
        JsonNode data = toolData("create_article_draft", Map.of(
                "title", "无标签草稿", "content", "正文", "sourceAgent", "opencode"));
        assertTrue(data.has("draftId"));
        assertTrue(data.has("ignoredTags"), "响应应恒含 ignoredTags: " + data);
        assertTrue(data.get("ignoredTags").isArray() && data.get("ignoredTags").isEmpty(), "" + data);
    }

    @Test
    @Order(33)
    @DisplayName("tools/call create_article_draft — tags 超过 5 个返回 error，不创建文章")
    void createArticleDraftTooManyTags() throws Exception {
        long before = articleService.countByStatus(null);
        JsonNode data = toolData("create_article_draft", Map.of(
                "title", "标签过多", "content", "正文", "sourceAgent", "opencode",
                "tags", java.util.List.of("a", "b", "c", "d", "e", "f")));
        assertTrue(data.has("error"), "" + data);
        assertEquals(before, articleService.countByStatus(null));
    }

    // ---- writing guide ----

    @Test
    @Order(40)
    @DisplayName("tools/call get_writing_guide — 返回站点配置的写作指南，未配置时为空字符串")
    void getWritingGuide() throws Exception {
        systemConfigService.saveWritingGuide("");
        assertEquals("", toolData("get_writing_guide", Map.of()).path("content").asText(null));

        String guide = "# 写作指南\n\n先写结论。";
        systemConfigService.saveWritingGuide(guide);
        assertEquals(guide, toolData("get_writing_guide", Map.of()).path("content").asText(null));
    }

    @Test
    @Order(41)
    @DisplayName("tools/list — 恰为 6 个 Tool")
    void toolsListExact() throws Exception {
        var names = parseResult(mcpCall("tools/list", null)).get("tools").findValuesAsText("name");
        assertEquals(java.util.Set.of(
                "create_article_draft", "create_note_draft", "search_articles", "get_article",
                "get_writing_guide", "get_tags"), new java.util.HashSet<>(names));
        assertEquals(6, names.size());
    }

    // ---- note MCP ----

    @Test
    @Order(21)
    @DisplayName("tools/call create_note_draft — 创建 note 草稿并返回 ID")
    void createNoteDraft() throws Exception {
        MvcResult result = mcpCall("tools/call", Map.of(
                "name", "create_note_draft",
                "arguments", Map.of(
                        "title", "端到端测试动态",
                        "content", "# 动态\n这是 AI 生成的测试动态。",
                        "type", "TECH",
                        "sourceAgent", "opencode"
                )
        ));
        JsonNode data = objectMapper.readTree(parseResult(result).get("content").get(0).get("text").asText());
        assertTrue(data.has("draftId"), "应返回 draftId");
        Long createdNoteId = data.get("draftId").asLong();
        assertTrue(createdNoteId > 0, "draftId 应为正数");
    }

    // ---- error handling ----

    @Test
    @Order(13)
    @DisplayName("未知 method 返回 -32601 Method not found")
    void unknownMethod() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-MCP-Key", "test-key")
                        .header(SESSION_HEADER, sessionId)
                        .content(rpc("nonexistent_method", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32601));
    }

    @Test
    @Order(14)
    @DisplayName("缺少 method 字段返回 -32600 Invalid Request")
    void missingMethod() throws Exception {
        String body = objectMapper.writeValueAsString(
                objectMapper.createObjectNode()
                        .put("jsonrpc", "2.0")
                        .put("id", 1)
        );
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-MCP-Key", "test-key")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32600));
    }

    @Test
    @Order(15)
    @DisplayName("tools/call 缺少 name 参数返回 -32602")
    void toolsCallMissingName() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-MCP-Key", "test-key")
                        .header(SESSION_HEADER, sessionId)
                        .content(rpc("tools/call", Map.of("arguments", Map.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602))
                .andExpect(jsonPath("$.error.message").value(containsString("name required")));
    }

    @Test
    @Order(16)
    @DisplayName("缺少 X-MCP-Key Header 返回 401")
    void missingApiKey() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rpc("initialize", null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(17)
    @DisplayName("缺少 Mcp-Session-Id Header 返回 -32000")
    void missingSessionId() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-MCP-Key", "test-key")
                        .content(rpc("tools/list", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(-32000));
    }

    @Test
    @Order(18)
    @DisplayName("tools/call 未知 Tool 返回 -32602")
    void unknownTool() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-MCP-Key", "test-key")
                        .header(SESSION_HEADER, sessionId)
                        .content(rpc("tools/call", Map.of("name", "nonexistent_tool"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602))
                .andExpect(jsonPath("$.error.message").value(containsString("Unknown tool")));
    }

    @Test
    @Order(23)
    @DisplayName("GET /mcp — 建立 SSE 流并立即收到 connected 注释（opencode 等客户端要求 GET 返回 200）")
    void sseStream() throws Exception {
        MvcResult result = mockMvc.perform(get("/mcp")
                        .header("X-MCP-Key", "test-key")
                        .header(SESSION_HEADER, sessionId)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains(":connected"), "SSE 流应包含 connected 注释: " + body);
    }

    @Test
    @Order(24)
    @DisplayName("GET /mcp — 未知会话返回 404")
    void sseStreamUnknownSession() throws Exception {
        mockMvc.perform(get("/mcp")
                        .header("X-MCP-Key", "test-key")
                        .header(SESSION_HEADER, "nonexistent-session-id")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isNotFound());
    }
}
