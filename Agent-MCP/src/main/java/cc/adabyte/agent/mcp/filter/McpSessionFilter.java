package cc.adabyte.agent.mcp.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(0)
public class McpSessionFilter extends OncePerRequestFilter {

    private static final String MCP_ENDPOINT = "/mcp";
    private static final String MCP_SESSION_ID_HEADER = "Mcp-Session-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);
        if (!request.getRequestURI().equals(MCP_ENDPOINT)) {
            return;
        }

        if (response.getHeader(MCP_SESSION_ID_HEADER) != null) {
            return;
        }

        String sessionId = request.getHeader(MCP_SESSION_ID_HEADER);
        if (sessionId != null && !sessionId.isBlank()) {
            response.setHeader(MCP_SESSION_ID_HEADER, sessionId);
        }
    }
}
