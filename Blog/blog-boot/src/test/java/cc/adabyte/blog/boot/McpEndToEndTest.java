package cc.adabyte.blog.boot;

import cc.adabyte.blog.common.gateway.McpKeyValidator;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Agent-MCP 端到端集成测试。
 * 覆盖完整链路：POST /mcp → initialize → tools/list → 4 个 Tool 调用。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("MCP 端到端集成测试")
class McpEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** Stub — 接受任意 API Key 以绕过认证 */
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public McpKeyValidator stubKeyValidator() {
            return key -> true;
        }
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
                        .content(rpc(method, params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andReturn();
    }

    private JsonNode parseResult(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("result");
    }

    // ---- tests ----

    @Test
    @Order(1)
    @DisplayName("initialize — 返回协议版本和服务器信息")
    void initialize() throws Exception {
        MvcResult result = mcpCall("initialize", Map.of(
                "protocolVersion", "2025-03-26",
                "clientInfo", Map.of("name", "test-client")
        ));
        JsonNode r = parseResult(result);

        assertEquals("2025-03-26", r.get("protocolVersion").asText());
        assertEquals("adawing-mcp-server", r.get("serverInfo").get("name").asText());
        assertTrue(r.has("capabilities"));
        assertTrue(r.get("capabilities").has("tools"));
    }

    @Test
    @Order(2)
    @DisplayName("tools/list — 返回 4 个已注册 Tool")
    void toolsList() throws Exception {
        MvcResult result = mcpCall("tools/list", null);
        JsonNode r = parseResult(result);

        assertTrue(r.has("tools"));
        assertEquals(4, r.get("tools").size());

        // 验证每个 Tool 的 name 字段
        var names = r.get("tools").findValuesAsText("name");
        assertTrue(names.contains("get_content_rules"));
        assertTrue(names.contains("create_article_draft"));
        assertTrue(names.contains("search_articles"));
        assertTrue(names.contains("get_article"));
    }

    @Test
    @Order(3)
    @DisplayName("tools/call get_content_rules — 返回字段规则约束")
    void getContentRules() throws Exception {
        MvcResult result = mcpCall("tools/call", Map.of(
                "name", "get_content_rules",
                "arguments", Map.of()
        ));
        JsonNode r = parseResult(result);

        assertTrue(r.has("content"));
        String text = r.get("content").get(0).get("text").asText();
        JsonNode rulesData = objectMapper.readTree(text);
        // get_content_rules 直接返回各字段定义作为顶层 key
        assertTrue(rulesData.has("title"), "应有 title 字段规则");
        assertTrue(rulesData.has("content"), "应有 content 字段规则");
        assertTrue(rulesData.has("summary"), "应有 summary 字段规则");
        assertTrue(rulesData.get("title").get("required").asBoolean());
    }

    private static Long createdDraftId;

    @Test
    @Order(4)
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

    @Test
    @Order(5)
    @DisplayName("tools/call get_article — 根据 ID 获取文章详情")
    void getArticle() throws Exception {
        assertNotNull(createdDraftId, "依赖上一个测试创建的草稿 ID");

        MvcResult result = mcpCall("tools/call", Map.of(
                "name", "get_article",
                "arguments", Map.of("id", createdDraftId)
        ));
        JsonNode r = parseResult(result);

        String text = r.get("content").get(0).get("text").asText();
        JsonNode data = objectMapper.readTree(text);

        assertFalse(data.has("error"), "不应返回错误: " + text);
        assertEquals(createdDraftId.longValue(), data.get("id").asLong());
        assertEquals("端到端测试文章", data.get("title").asText());
        assertTrue(data.get("content").asText().contains("# 测试"));
    }

    @Test
    @Order(6)
    @DisplayName("tools/call search_articles — 按关键词搜索文章")
    void searchArticles() throws Exception {
        assertNotNull(createdDraftId, "依赖创建草稿测试");

        MvcResult result = mcpCall("tools/call", Map.of(
                "name", "search_articles",
                "arguments", Map.of("keyword", "端到端测试", "limit", 10)
        ));
        JsonNode r = parseResult(result);

        String text = r.get("content").get(0).get("text").asText();
        JsonNode data = objectMapper.readTree(text);

        assertTrue(data.has("items"));
        assertTrue(data.get("items").size() > 0, "应至少搜索到一篇匹配文章");
        // 验证搜索结果的 ID 匹配
        var ids = data.get("items").findValuesAsText("id");
        assertTrue(ids.contains(String.valueOf(createdDraftId)));
    }

    // ---- error handling ----

    @Test
    @Order(7)
    @DisplayName("未知 method 返回 -32601 Method not found")
    void unknownMethod() throws Exception {
        MvcResult result = mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-MCP-Key", "test-key")
                        .content(rpc("nonexistent_method", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32601))
                .andReturn();
    }

    @Test
    @Order(8)
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
    @Order(9)
    @DisplayName("tools/call 缺少 name 参数返回 -32602")
    void toolsCallMissingName() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-MCP-Key", "test-key")
                        .content(rpc("tools/call", Map.of("arguments", Map.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602))
                .andExpect(jsonPath("$.error.message").value(containsString("name required")));
    }

    @Test
    @Order(10)
    @DisplayName("缺少 X-MCP-Key Header 返回 401")
    void missingApiKey() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rpc("initialize", null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(11)
    @DisplayName("tools/call 未知 Tool 返回 -32602")
    void unknownTool() throws Exception {
        mockMvc.perform(post("/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-MCP-Key", "test-key")
                        .content(rpc("tools/call", Map.of("name", "nonexistent_tool"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602))
                .andExpect(jsonPath("$.error.message").value(containsString("Unknown tool")));
    }
}
