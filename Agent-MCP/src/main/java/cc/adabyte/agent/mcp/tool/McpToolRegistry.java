package cc.adabyte.agent.mcp.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class McpToolRegistry {

    private final Map<String, McpTool> tools = new LinkedHashMap<>();

    public McpToolRegistry(List<McpTool> toolList) {
        if (toolList != null) {
            toolList.forEach(t -> tools.put(t.getName(), t));
        }
        log.info("[MCP] Registered {} tools: {}", tools.size(), tools.keySet());
    }

    public List<McpToolDefinition> listTools() {
        return tools.values().stream()
                .map(t -> McpToolDefinition.builder()
                        .name(t.getName())
                        .description(t.getDescription())
                        .inputSchema(t.getInputSchema())
                        .build())
                .toList();
    }

    public Optional<McpTool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }
}
