package cc.adabyte.blog.system.auth.filter;

import cc.adabyte.blog.common.constants.AuthConstants;
import cc.adabyte.blog.system.auth.enums.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * 管理员权限授权过滤器。
 *
 * <p>在 {@link JwtAuthenticationFilter} 之后执行，对管理端端点显式校验当前用户角色为 ADMIN。
 * 本项目为单管理员部署，任何通过 JWT 认证的活跃用户默认具备 ADMIN 角色；该过滤器显式声明
 * 这些端点的管理员属性，并为未来多角色扩展预留位置。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
@RequiredArgsConstructor
public class AdminAuthorizationFilter extends OncePerRequestFilter {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final List<AdminPattern> adminPatterns = List.of(
            new AdminPattern("/api/v2/auth/change-password", Set.of("POST")),
            new AdminPattern("/api/v2/mcp-keys/**", null),
            new AdminPattern("/api/v2/articles/admin/**", null),
            new AdminPattern("/api/v2/articles", Set.of("POST")),
            new AdminPattern("/api/v2/articles/*/publish", Set.of("POST")),
            new AdminPattern("/api/v2/articles/*/review", Set.of("POST")),
            new AdminPattern("/api/v2/articles/*/hide", Set.of("POST")),
            new AdminPattern("/api/v2/articles/*", Set.of("DELETE")),
            new AdminPattern("/api/v2/messages/admin/**", null),
            new AdminPattern("/api/v2/messages/*/approve", Set.of("POST")),
            new AdminPattern("/api/v2/messages/*/reject", Set.of("POST")),
            new AdminPattern("/api/v2/messages/*", Set.of("DELETE")),
            new AdminPattern("/api/v2/review/**", null),
            new AdminPattern("/api/v2/notes", Set.of("POST", "PUT", "DELETE")),
            new AdminPattern("/api/v2/notes/*", Set.of("PUT", "DELETE")),
            new AdminPattern("/api/v2/admin/resources/**", null),
            new AdminPattern("/api/v2/config/site", Set.of("PUT")),
            new AdminPattern("/api/v2/tags", Set.of("POST")),
            new AdminPattern("/api/v2/tags/*/merge/*", Set.of("POST"))
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isAdminEndpoint(method, uri)) {
            UserRole role = (UserRole) request.getAttribute(AuthConstants.CURRENT_ROLE_ATTRIBUTE);
            if (role != UserRole.ADMIN) {
                log.warn("越权访问管理端点: {} {} (role={})", method, uri, role);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Forbidden");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAdminEndpoint(String method, String uri) {
        for (AdminPattern pattern : adminPatterns) {
            if (!pathMatcher.match(pattern.pattern(), uri)) {
                continue;
            }
            if (pattern.methods() == null || pattern.methods().contains(method)) {
                return true;
            }
        }
        return false;
    }

    private record AdminPattern(String pattern, Set<String> methods) {}
}
