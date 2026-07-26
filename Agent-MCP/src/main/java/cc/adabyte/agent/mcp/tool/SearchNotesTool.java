package cc.adabyte.agent.mcp.tool;

import cc.adabyte.blog.common.gateway.NoteQueryGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SearchNotesTool implements McpTool {

    private final NoteQueryGateway queryGateway;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "search_notes";
    }

    @Override
    public String getDescription() {
        return "按关键词搜索已发布的动态（note）";
    }

    @Override
    public JsonNode getInputSchema() {
        return objectMapper.valueToTree(Map.of(
                "type", "object",
                "properties", Map.of(
                        "keyword", Map.of("type", "string"),
                        "limit", Map.of("type", "integer", "default", 10)
                ),
                "required", List.of("keyword")
        ));
    }

    @Override
    public Object execute(JsonNode arguments) {
        String keyword = arguments.path("keyword").asText();
        int rawLimit = arguments.has("limit") ? arguments.get("limit").asInt(10) : 10;
        int limit = Math.min(rawLimit, 20);
        List<NoteQueryGateway.Result> results = queryGateway.search(keyword, limit);
        List<Map<String, Object>> items = results.stream()
                .map(r -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", r.id());
                    item.put("title", r.title());
                    item.put("type", r.type());
                    return item;
                })
                .toList();
        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", results.size());
        result.put("capped", rawLimit > 20);
        return result;
    }
}
