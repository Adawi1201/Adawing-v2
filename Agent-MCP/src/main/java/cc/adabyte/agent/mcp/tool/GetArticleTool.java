package cc.adabyte.agent.mcp.tool;

import cc.adabyte.blog.common.gateway.ArticleQueryGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetArticleTool implements McpTool {

    private final ArticleQueryGateway queryGateway;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "get_article";
    }

    @Override
    public String getDescription() {
        return "根据 ID 获取文章详情（含 Markdown 原文）";
    }

    @Override
    public JsonNode getInputSchema() {
        return objectMapper.valueToTree(Map.of(
                "type", "object",
                "properties", Map.of("id", Map.of("type", "integer")),
                "required", List.of("id")
        ));
    }

    @Override
    public Object execute(JsonNode arguments) {
        if (!arguments.has("id") || arguments.get("id").isNull()) {
            return Map.of("error", "Missing required parameter: id");
        }
        long id = arguments.get("id").asLong();
        ArticleQueryGateway.Result result = queryGateway.getById(id);
        if (result == null) {
            return Map.of("error", "Article not found: " + id);
        }
        // 使用 HashMap 而非 Map.of() 以兼容 null 值（如 content 可能因编码问题为空）
        Map<String, Object> data = new HashMap<>();
        data.put("id", result.id());
        data.put("title", result.title());
        data.put("content", result.content());
        data.put("summary", result.summary());
        data.put("sourceAgent", result.sourceAgent() != null ? result.sourceAgent() : "");
        return data;
    }
}
