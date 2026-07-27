package cc.adabyte.agent.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcResponse {
    private String jsonrpc = "2.0";
    private Object id;
    private Object result;
    private JsonRpcError error;

    public static JsonRpcResponse ok(Object id, Object result) {
        JsonRpcResponse r = new JsonRpcResponse();
        r.setId(id);
        r.setResult(result);
        return r;
    }

    public static JsonRpcResponse error(Object id, int code, String message) {
        JsonRpcResponse r = new JsonRpcResponse();
        r.setId(id);
        r.setError(new JsonRpcError(code, message));
        return r;
    }
}
