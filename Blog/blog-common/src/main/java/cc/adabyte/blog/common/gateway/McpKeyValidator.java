package cc.adabyte.blog.common.gateway;

/**
 * 防腐层接口 — MCP Key 校验。
 * agent-mcp 通过此接口校验 key，不直接依赖 system-auth 的 McpKeyService。
 */
public interface McpKeyValidator {
    boolean validateKey(String key);
}
