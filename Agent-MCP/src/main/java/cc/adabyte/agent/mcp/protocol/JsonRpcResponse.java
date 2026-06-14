package cc.adabyte.agent.mcp.protocol;

import lombok.Data;

@Data
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
