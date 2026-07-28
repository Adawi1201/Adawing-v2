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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/mcp")
public class McpServerController {

    private static final String MCP_SESSION_ID_HEADER = "Mcp-Session-Id";
    private static final String DEFAULT_PROTOCOL_VERSION = "2025-03-26";
    private static final java.util.Set<String> SUPPORTED_PROTOCOL_VERSIONS =
            java.util.Set.of("2024-11-05", "2025-03-26", "2025-06-18");
    private static final String CONTENT_RULES_URI = "adawing://content-rules";

    private final McpToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final McpSessionStore sessionStore;

    private static final long SSE_HEARTBEAT_SECONDS = 25;
    private final ScheduledExecutorService sseHeartbeatExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "mcp-sse-heartbeat");
                t.setDaemon(true);
                return t;
            });

    /**
     * Streamable HTTP 的可选 GET 流。本服务不做服务端主动推送，
     * 流仅保持挂起并周期发送心跳注释，供严格要求 GET 返回 200 的客户端（如 opencode）建立连接。
     * 未携带会话 Header 时允许匿名挂起；携带但未知时返回 404。
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(HttpServletRequest request) {
        String sessionId = request.getHeader(MCP_SESSION_ID_HEADER);
        if (sessionId != null && !sessionId.isBlank() && sessionStore.touchSession(sessionId).isEmpty()) {
            log.warn("[MCP] SSE stream rejected: unknown sessionId={}", sessionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        SseEmitter emitter = new SseEmitter(0L);
        ScheduledFuture<?> heartbeat = sseHeartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }, SSE_HEARTBEAT_SECONDS, SSE_HEARTBEAT_SECONDS, TimeUnit.SECONDS);

        emitter.onCompletion(() -> heartbeat.cancel(false));
        emitter.onTimeout(() -> heartbeat.cancel(false));
        emitter.onError(e -> heartbeat.cancel(false));

        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        log.info("[MCP] SSE stream opened sessionId={}", sessionId);
        return ResponseEntity.ok(emitter);
    }

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
            case "ping" -> ResponseEntity.ok(JsonRpcResponse.ok(req.getId(), Map.of()));
            case "tools/list" -> handleToolsList(req, session);
            case "tools/call" -> handleToolsCall(req, session);
            case "resources/list" -> handleResourcesList(req);
            case "resources/read" -> handleResourcesRead(req);
            case "prompts/list" -> ResponseEntity.ok(JsonRpcResponse.ok(req.getId(), Map.of("prompts", List.of())));
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
        String clientVersion = readText(req.getParams(), "protocolVersion", null);
        String negotiated = clientVersion != null && SUPPORTED_PROTOCOL_VERSIONS.contains(clientVersion)
                ? clientVersion
                : DEFAULT_PROTOCOL_VERSION;
        return ResponseEntity.ok(JsonRpcResponse.ok(req.getId(), Map.of(
                "protocolVersion", negotiated,
                "serverInfo", Map.of("name", "adawing-mcp-server", "version", "1.1.0"),
                "capabilities", Map.of("tools", Map.of(), "resources", Map.of())
        )));
    }

    private ResponseEntity<?> handleResourcesList(JsonRpcRequest req) {
        Map<String, Object> resource = new HashMap<>();
        resource.put("uri", CONTENT_RULES_URI);
        resource.put("name", "Content Rules");
        resource.put("description", "文章/内容创作的字段规则与约束（与 get_content_rules 工具一致）");
        resource.put("mimeType", "application/json");
        return ResponseEntity.ok(JsonRpcResponse.ok(req.getId(), Map.of("resources", List.of(resource))));
    }

    private ResponseEntity<?> handleResourcesRead(JsonRpcRequest req) {
        JsonNode params = req.getParams();
        String uri = params != null && params.has("uri") ? params.get("uri").asText() : null;
        if (!CONTENT_RULES_URI.equals(uri)) {
            return ResponseEntity.ok(JsonRpcResponse.error(req.getId(), -32602, "Unknown resource: " + uri));
        }
        try {
            McpTool rulesTool = toolRegistry.getTool("get_content_rules")
                    .orElseThrow(() -> new IllegalStateException("get_content_rules tool not registered"));
            String text = objectMapper.writeValueAsString(rulesTool.execute(null));
            Map<String, Object> content = new HashMap<>();
            content.put("uri", CONTENT_RULES_URI);
            content.put("mimeType", "application/json");
            content.put("text", text);
            return ResponseEntity.ok(JsonRpcResponse.ok(req.getId(), Map.of("contents", List.of(content))));
        } catch (Exception e) {
            log.error("[MCP] resources/read failed uri={}, id={}", uri, req.getId(), e);
            return ResponseEntity.ok(JsonRpcResponse.error(req.getId(), -32603, e.getMessage()));
        }
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
