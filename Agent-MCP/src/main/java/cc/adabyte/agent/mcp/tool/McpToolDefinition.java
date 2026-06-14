package cc.adabyte.agent.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class McpToolDefinition {
    private String name;
    private String description;
    private JsonNode inputSchema;
}
