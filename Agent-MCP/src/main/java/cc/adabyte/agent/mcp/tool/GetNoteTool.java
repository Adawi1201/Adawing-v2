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
public class GetNoteTool implements McpTool {

    private final NoteQueryGateway queryGateway;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "get_note";
    }

    @Override
    public String getDescription() {
        return "根据 ID 获取动态（note）详情（含 Markdown 原文）";
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
        NoteQueryGateway.Result result = queryGateway.getById(id);
        if (result == null) {
            return Map.of("error", "Note not found: " + id);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", result.id());
        data.put("title", result.title());
        data.put("content", result.content());
        data.put("type", result.type());
        data.put("sourceAgent", result.sourceAgent() != null ? result.sourceAgent() : "");
        return data;
    }
}
