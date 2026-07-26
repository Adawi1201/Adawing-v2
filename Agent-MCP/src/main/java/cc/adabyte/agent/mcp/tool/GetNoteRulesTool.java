package cc.adabyte.agent.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetNoteRulesTool implements McpTool {

    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "get_note_rules";
    }

    @Override
    public String getDescription() {
        return "获取动态（note）创作的字段规则与约束";
    }

    @Override
    public JsonNode getInputSchema() {
        return objectMapper.valueToTree(Map.of(
                "type", "object",
                "properties", Map.of(),
                "required", List.of()
        ));
    }

    @Override
    public Object execute(JsonNode arguments) {
        return Map.of(
                "title", Map.of("maxLength", 256, "required", false),
                "content", Map.of("format", "markdown", "required", true),
                "type", Map.of("required", true, "enum", new String[]{"PERSONAL", "TECH"}),
                "sourceAgent", Map.of("required", true, "enum", new String[]{"codex", "claude-code", "openclaw", "opencode"}),
                "note", "AI 生成的动态需经人工审核后才会发布"
        );
    }
}
