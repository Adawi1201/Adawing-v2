package cc.adabyte.agent.mcp.tool;

import cc.adabyte.blog.common.gateway.WritingGuideGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetWritingGuideTool implements McpTool {

    private final WritingGuideGateway writingGuideGateway;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "get_writing_guide";
    }

    @Override
    public String getDescription() {
        return "获取站点维护的写作指南（Markdown），起草文章或动态前调用并遵循";
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
        return Map.of("content", writingGuideGateway.getWritingGuide());
    }
}
