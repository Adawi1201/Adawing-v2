package cc.adabyte.agent.mcp.tool;

import cc.adabyte.blog.common.gateway.ContentDraftGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CreateArticleDraftTool implements McpTool {

    private final ContentDraftGateway contentDraftGateway;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "create_article_draft";
    }

    @Override
    public String getDescription() {
        return "创建文章草稿，返回草稿 ID";
    }

    @Override
    public JsonNode getInputSchema() {
        return objectMapper.valueToTree(Map.of(
                "type", "object",
                "properties", Map.of(
                        "title", Map.of("type", "string", "maxLength", 256),
                        "content", Map.of("type", "string"),
                        "summary", Map.of("type", "string", "maxLength", 512),
                        "sourceAgent", Map.of("type", "string", "enum", new String[]{"codex", "claude-code", "openclaw"})
                ),
                "required", List.of("title", "content", "sourceAgent")
        ));
    }

    @Override
    public Object execute(JsonNode arguments) {
        String title = arguments.path("title").asText();
        String content = arguments.path("content").asText();
        String summary = arguments.has("summary") ? arguments.get("summary").asText() : null;
        String sourceAgent = arguments.path("sourceAgent").asText();
        Long draftId = contentDraftGateway.createArticleDraft(title, content, summary, sourceAgent);
        return Map.of("draftId", draftId);
    }
}
