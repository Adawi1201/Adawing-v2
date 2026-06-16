package cc.adabyte.agent.mcp.session;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryMcpSessionStore implements McpSessionStore {

    private final ConcurrentMap<String, McpSession> sessions = new ConcurrentHashMap<>();

    @Override
    public McpSession createSession(String protocolVersion, String clientName, String clientVersion) {
        Instant now = Instant.now();
        McpSession session = McpSession.builder()
                .sessionId(UUID.randomUUID().toString())
                .protocolVersion(protocolVersion)
                .clientName(clientName)
                .clientVersion(clientVersion)
                .createdAt(now)
                .lastSeenAt(now)
                .build();
        sessions.put(session.getSessionId(), session);
        return session;
    }

    @Override
    public Optional<McpSession> getSession(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public Optional<McpSession> touchSession(String sessionId) {
        return Optional.ofNullable(sessions.computeIfPresent(sessionId, (id, session) ->
                session.toBuilder()
                        .lastSeenAt(Instant.now())
                        .build()
        ));
    }
}
