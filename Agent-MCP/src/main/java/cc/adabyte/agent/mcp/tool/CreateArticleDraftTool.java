package cc.adabyte.agent.mcp.tool;

import cc.adabyte.blog.common.gateway.ContentDraftGateway;
import cc.adabyte.blog.common.gateway.TagQueryGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CreateArticleDraftTool implements McpTool {

    private static final int MAX_TAGS = 5;

    private final ContentDraftGateway contentDraftGateway;
    private final TagQueryGateway tagQueryGateway;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "create_article_draft";
    }

    @Override
    public String getDescription() {
        return "创建文章草稿，进入人工审核，返回草稿 ID。起草前先调用 get_writing_guide；tags 只能取自 get_tags，未命中的会在 ignoredTags 中返回";
    }

    @Override
    public JsonNode getInputSchema() {
        return objectMapper.valueToTree(Map.of(
                "type", "object",
                "properties", Map.of(
                        "title", Map.of("type", "string", "maxLength", 256),
                        "content", Map.of("type", "string"),
                        "summary", Map.of("type", "string", "maxLength", 512),
                        "tags", Map.of("type", "array", "items", Map.of("type", "string"), "maxItems", MAX_TAGS),
                        "sourceAgent", Map.of("type", "string", "enum", new String[]{"codex", "claude-code", "openclaw", "opencode"})
                ),
                "required", List.of("title", "content", "sourceAgent")
        ));
    }

    @Override
    public Object execute(JsonNode arguments) {
        JsonNode tagsNode = arguments.path("tags");
        if (!tagsNode.isMissingNode() && !tagsNode.isNull() && !tagsNode.isArray()) {
            return Map.of("error", "tags 必须是字符串数组");
        }
        if (tagsNode.size() > MAX_TAGS) {
            return Map.of("error", "tags 最多 " + MAX_TAGS + " 个");
        }

        Set<String> requested = new LinkedHashSet<>();
        tagsNode.forEach(n -> {
            String name = n.asText().trim();
            if (!name.isEmpty()) requested.add(name);
        });
        Set<String> existing = new HashSet<>(tagQueryGateway.listNames());
        List<String> matched = new ArrayList<>();
        List<String> ignored = new ArrayList<>();
        for (String name : requested) {
            (existing.contains(name) ? matched : ignored).add(name);
        }

        String title = arguments.path("title").asText();
        String content = arguments.path("content").asText();
        String summary = arguments.has("summary") ? arguments.get("summary").asText() : null;
        String sourceAgent = arguments.path("sourceAgent").asText();
        Long draftId = contentDraftGateway.createArticleDraft(title, content, summary, sourceAgent, matched);
        return Map.of("draftId", draftId, "ignoredTags", ignored);
    }
}
