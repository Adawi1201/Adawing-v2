package cc.adabyte.agent.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetContentRulesTool implements McpTool {

    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "get_content_rules";
    }

    @Override
    public String getDescription() {
        return "获取文章/内容创作的字段规则与约束";
    }

    @Override
    public JsonNode getInputSchema() {
        return objectMapper.valueToTree(Map.of(
                "type", "object",
                "properties", Map.of(),
                "required", java.util.List.of()
        ));
    }

    @Override
    public Object execute(JsonNode arguments) {
        return Map.of(
                "title", Map.of("maxLength", 256, "required", true),
                "summary", Map.of("maxLength", 512, "required", false),
                "content", Map.of("format", "markdown", "required", true),
                "tags", Map.of("maxCount", 5, "allowCreate", false),
                "sourceAgent", Map.of("required", true, "enum", new String[]{"codex", "claude-code", "openclaw"})
        );
    }
}
