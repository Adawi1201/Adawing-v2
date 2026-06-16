package cc.adabyte.agent.mcp.session;

import java.util.Optional;

public interface McpSessionStore {

    McpSession createSession(String protocolVersion, String clientName, String clientVersion);

    Optional<McpSession> getSession(String sessionId);

    Optional<McpSession> touchSession(String sessionId);
}
