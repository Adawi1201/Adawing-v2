package cc.adabyte.agent.mcp.interceptor;

import cc.adabyte.blog.common.gateway.McpKeyValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class McpAuthInterceptor implements HandlerInterceptor {

    private final McpKeyValidator keyValidator;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String key = req.getHeader("X-MCP-Key");
        if (key == null || key.isBlank()) {
            res.setStatus(401);
            return false;
        }
        if (!keyValidator.validateKey(key)) {
            res.setStatus(401);
            return false;
        }
        return true;
    }
}
