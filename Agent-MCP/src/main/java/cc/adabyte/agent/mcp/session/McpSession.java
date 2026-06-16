package cc.adabyte.agent.mcp.session;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
public class McpSession {
    String sessionId;
    String protocolVersion;
    String clientName;
    String clientVersion;
    Instant createdAt;
    Instant lastSeenAt;
}
