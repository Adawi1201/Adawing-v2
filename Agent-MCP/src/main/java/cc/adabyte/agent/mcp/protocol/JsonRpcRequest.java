package cc.adabyte.agent.mcp.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class JsonRpcRequest {
    private String jsonrpc = "2.0";
    private Object id;
    private String method;
    private JsonNode params;
}
