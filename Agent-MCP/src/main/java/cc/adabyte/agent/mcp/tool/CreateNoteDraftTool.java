package cc.adabyte.agent.mcp.tool;

import cc.adabyte.blog.common.gateway.NoteDraftGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CreateNoteDraftTool implements McpTool {

    private final NoteDraftGateway noteDraftGateway;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "create_note_draft";
    }

    @Override
    public String getDescription() {
        return "创建动态（note）草稿，进入人工审核，返回草稿 ID";
    }

    @Override
    public JsonNode getInputSchema() {
        return objectMapper.valueToTree(Map.of(
                "type", "object",
                "properties", Map.of(
                        "title", Map.of("type", "string", "maxLength", 256),
                        "content", Map.of("type", "string"),
                        "type", Map.of("type", "string", "enum", new String[]{"PERSONAL", "TECH"}),
                        "sourceAgent", Map.of("type", "string", "enum", new String[]{"codex", "claude-code", "openclaw", "opencode"})
                ),
                "required", List.of("content", "type", "sourceAgent")
        ));
    }

    @Override
    public Object execute(JsonNode arguments) {
        String title = arguments.has("title") ? arguments.get("title").asText() : null;
        String content = arguments.path("content").asText();
        String type = arguments.path("type").asText();
        String sourceAgent = arguments.path("sourceAgent").asText();
        Long draftId = noteDraftGateway.createNoteDraft(title, content, type, sourceAgent);
        return Map.of("draftId", draftId);
    }
}
