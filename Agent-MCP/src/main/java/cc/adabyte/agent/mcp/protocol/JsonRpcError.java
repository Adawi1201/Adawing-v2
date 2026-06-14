package cc.adabyte.agent.mcp.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JsonRpcError {
    private int code;
    private String message;
}
