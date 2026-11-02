package cc.adabyte.agent.mcp.tool;

import cc.adabyte.blog.common.gateway.TagQueryGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetTagsTool implements McpTool {

    private final TagQueryGateway tagQueryGateway;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "get_tags";
    }

    @Override
    public String getDescription() {
        return "获取站点已有的标签名列表。create_article_draft 的 tags 只能从中选择";
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
        return Map.of("tags", tagQueryGateway.listNames());
    }
}
