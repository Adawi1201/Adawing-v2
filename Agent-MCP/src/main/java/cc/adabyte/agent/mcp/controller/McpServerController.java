package cc.adabyte.agent.mcp.controller;

import cc.adabyte.agent.mcp.protocol.JsonRpcRequest;
import cc.adabyte.agent.mcp.protocol.JsonRpcResponse;
import cc.adabyte.agent.mcp.session.McpSession;
import cc.adabyte.agent.mcp.session.McpSessionStore;
import cc.adabyte.agent.mcp.tool.McpTool;
import cc.adabyte.agent.mcp.tool.McpToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private static final String MCP_SESSION_ID_HEADER = "Mcp-Session-Id";

    private final McpToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final McpSessionStore sessionStore;

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public JsonRpcResponse handleException(Exception e) {
        log.error("[MCP] Unhandled exception", e);
        return JsonRpcResponse.error(null, -32603, e.getMessage() != null ? e.getMessage() : "Internal error");
    }

    @PostMapping
    public ResponseEntity<?> handle(@RequestBody JsonRpcRequest req,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        String method = req.getMethod();
        if (method == null) {
            log.warn("[MCP] Invalid request: method missing, id={}", req.getId());
            return ResponseEntity.ok(JsonRpcResponse.error(req.getId(), -32600, "Invalid Request: method missing"));
        }

        if (method.startsWith("notifications/")) {
            log.debug("[MCP] Notification: {}", method);
            return ResponseEntity.accepted().build();
        }

        McpSession session = null;
        if ("initialize".equals(method)) {
            session = createSession(req, response);
        } else {
            String sessionId = request.getHeader(MCP_SESSION_ID_HEADER);
            if (sessionId == null || sessionId.isBlank()) {
                log.warn("[MCP] Missing session header method={}, id={}", method, req.getId());
                return ResponseEntity.badRequest()
                        .body(JsonRpcResponse.error(req.getId(), -32000, "Missing Mcp-Session-Id header"));
            }
            session = sessionStore.touchSession(sessionId).orElse(null);
            if (session == null) {
                log.warn("[MCP] Unknown session method={}, id={}, sessionId={}", method, req.getId(), sessionId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(JsonRpcResponse.error(req.getId(), -32001, "Unknown MCP session"));
            }
        }

        log.debug("[MCP] Request method={}, id={}, sessionId={}, client={}/{}",
                method, req.getId(), session.getSessionId(), session.getClientName(), session.getClientVersion());

        return switch (method) {
            case "initialize" -> handleInitialize(req);
            case "tools/list" -> handleToolsList(req, session);
            case "tools/call" -> handleToolsCall(req, session);
            default -> {
                log.warn("[MCP] Method not found: {}, id={}, sessionId={}", method, req.getId(), session.getSessionId());
                yield ResponseEntity.ok(JsonRpcResponse.error(req.getId(), -32601, "Method not found: " + method));
            }
        };
    }

    private McpSession createSession(JsonRpcRequest req, HttpServletResponse response) {
        JsonNode params = req.getParams();
        String protocolVersion = readText(params, "protocolVersion", "unknown");
        JsonNode clientInfo = params != null ? params.get("clientInfo") : null;
        String clientName = readText(clientInfo, "name", "unknown");
        String clientVersion = readText(clientInfo, "version", "unknown");
        McpSession session = sessionStore.createSession(protocolVersion, clientName, clientVersion);
        response.setHeader(MCP_SESSION_ID_HEADER, session.getSessionId());
        log.info("[MCP] Session created sessionId={}, client={}/{}, protocolVersion={}",
                session.getSessionId(), clientName, clientVersion, protocolVersion);
        return session;
    }

    private String readText(JsonNode node, String fieldName, String fallback) {
        if (node == null) {
            return fallback;
        }
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return fallback;
        }
        String text = field.asText();
        return text == null || text.isBlank() ? fallback : text;
    }

    private ResponseEntity<?> handleInitialize(JsonRpcRequest req) {
        return ResponseEntity.ok(JsonRpcResponse.ok(req.getId(), Map.of(
                "protocolVersion", "2025-03-26",
                "serverInfo", Map.of("name", "adawing-mcp-server", "version", "1.0.0"),
                "capabilities", Map.of("tools", Map.of())
        )));
    }

    private ResponseEntity<?> handleToolsList(JsonRpcRequest req, McpSession session) {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode toolsNode = objectMapper.valueToTree(toolRegistry.listTools());
        result.set("tools", toolsNode);
        log.info("[MCP] Tool list sessionId={}, count={}, id={}",
                session.getSessionId(), toolRegistry.listTools().size(), req.getId());
        return ResponseEntity.ok(JsonRpcResponse.ok(req.getId(), result));
    }

    private ResponseEntity<?> handleToolsCall(JsonRpcRequest req, McpSession session) {
        JsonNode params = req.getParams();
        if (params == null || !params.has("name")) {
            log.warn("[MCP] Invalid params: name required, id={}, sessionId={}", req.getId(), session.getSessionId());
            return ResponseEntity.ok(JsonRpcResponse.error(req.getId(), -32602, "Invalid params: name required"));
        }
        String toolName = params.get("name").asText();
        JsonNode arguments = params.has("arguments") ? params.get("arguments") : null;
        log.info("[MCP] Tool call start sessionId={}, client={}/{}, name={}, id={}",
                session.getSessionId(), session.getClientName(), session.getClientVersion(), toolName, req.getId());

        return toolRegistry.getTool(toolName)
                .map(tool -> {
                    long start = System.currentTimeMillis();
                    try {
                        Object output = tool.execute(arguments);
                        long duration = System.currentTimeMillis() - start;
                        log.info("[MCP] Tool call success sessionId={}, name={}, duration={}ms, id={}",
                                session.getSessionId(), toolName, duration, req.getId());
                        Map<String, Object> result = new HashMap<>();
                        result.put("content", List.of(Map.of("type", "text", "text", objectMapper.writeValueAsString(output))));
                        return ResponseEntity.ok(JsonRpcResponse.ok(req.getId(), result));
                    } catch (Exception e) {
                        long duration = System.currentTimeMillis() - start;
                        log.error("[MCP] Tool call failed sessionId={}, name={}, duration={}ms, id={}",
                                session.getSessionId(), toolName, duration, req.getId(), e);
                        return ResponseEntity.ok(JsonRpcResponse.error(req.getId(), -32603, e.getMessage()));
                    }
                })
                .orElseGet(() -> {
                    log.warn("[MCP] Unknown tool: {}, id={}, sessionId={}", toolName, req.getId(), session.getSessionId());
                    return ResponseEntity.ok(JsonRpcResponse.error(req.getId(), -32602, "Unknown tool: " + toolName));
                });
    }
}
