package cc.adabyte.agent.mcp.tool;

import com.fasterxml.jackson.databind.JsonNode;

public interface McpTool {
    String getName();
    String getDescription();
    JsonNode getInputSchema();
    Object execute(JsonNode arguments);
}
