package cc.adabyte.agent.mcp.controller;

import cc.adabyte.agent.mcp.protocol.JsonRpcRequest;
import cc.adabyte.agent.mcp.protocol.JsonRpcResponse;
import cc.adabyte.agent.mcp.tool.McpTool;
import cc.adabyte.agent.mcp.tool.McpToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/mcp")
public class McpServerController {

    private final McpToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    /**
     * 兜底异常处理 — 确保所有逃逸异常以 JSON-RPC 格式返回，
     * 避免被 blog-boot 的 GlobalExceptionHandler 转为 Result 格式。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public JsonRpcResponse handleException(Exception e) {
        log.error("[MCP] Unhandled exception", e);
        return JsonRpcResponse.error(null, -32603, e.getMessage() != null ? e.getMessage() : "Internal error");
    }

    @PostMapping
    public JsonRpcResponse handle(@RequestBody JsonRpcRequest req) {
        String method = req.getMethod();
        if (method == null) {
            log.warn("[MCP] Invalid request: method missing, id={}", req.getId());
            return JsonRpcResponse.error(req.getId(), -32600, "Invalid Request: method missing");
        }
        log.debug("[MCP] Request method={}, id={}", method, req.getId());

        return switch (method) {
            case "initialize" -> handleInitialize(req);
            case "tools/list" -> handleToolsList(req);
            case "tools/call" -> handleToolsCall(req);
            default -> {
                log.warn("[MCP] Method not found: {}, id={}", method, req.getId());
                yield JsonRpcResponse.error(req.getId(), -32601, "Method not found: " + method);
            }
        };
    }

    private JsonRpcResponse handleInitialize(JsonRpcRequest req) {
        return JsonRpcResponse.ok(req.getId(), Map.of(
                "protocolVersion", "2025-03-26",
                "serverInfo", Map.of("name", "adawing-mcp-server", "version", "1.0.0"),
                "capabilities", Map.of("tools", Map.of())
        ));
    }

    private JsonRpcResponse handleToolsList(JsonRpcRequest req) {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode toolsNode = objectMapper.valueToTree(toolRegistry.listTools());
        result.set("tools", toolsNode);
        return JsonRpcResponse.ok(req.getId(), result);
    }

    private JsonRpcResponse handleToolsCall(JsonRpcRequest req) {
        JsonNode params = req.getParams();
        if (params == null || !params.has("name")) {
            log.warn("[MCP] Invalid params: name required, id={}", req.getId());
            return JsonRpcResponse.error(req.getId(), -32602, "Invalid params: name required");
        }
        String toolName = params.get("name").asText();
        JsonNode arguments = params.has("arguments") ? params.get("arguments") : null;
        log.info("[MCP] Tool call start name={}, id={}", toolName, req.getId());

        return toolRegistry.getTool(toolName)
                .map(tool -> {
                    long start = System.currentTimeMillis();
                    try {
                        Object output = tool.execute(arguments);
                        long duration = System.currentTimeMillis() - start;
                        log.info("[MCP] Tool call success name={}, duration={}ms, id={}", toolName, duration, req.getId());
                        Map<String, Object> result = new HashMap<>();
                        result.put("content", List.of(Map.of("type", "text", "text", objectMapper.writeValueAsString(output))));
                        return JsonRpcResponse.ok(req.getId(), result);
                    } catch (Exception e) {
                        long duration = System.currentTimeMillis() - start;
                        log.error("[MCP] Tool call failed name={}, duration={}ms, id={}", toolName, duration, req.getId(), e);
                        return JsonRpcResponse.error(req.getId(), -32603, e.getMessage());
                    }
                })
                .orElseGet(() -> {
                    log.warn("[MCP] Unknown tool: {}, id={}", toolName, req.getId());
                    return JsonRpcResponse.error(req.getId(), -32602, "Unknown tool: " + toolName);
                });
    }
}
