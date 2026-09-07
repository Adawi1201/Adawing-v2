package cc.adabyte.blog.system.auth.filter;

import cc.adabyte.blog.common.constants.AuthConstants;
import cc.adabyte.blog.common.result.Result;
import cc.adabyte.blog.common.util.JwtUtil;
import cc.adabyte.blog.system.auth.entity.SysUser;
import cc.adabyte.blog.system.auth.enums.UserRole;
import cc.adabyte.blog.system.auth.enums.UserStatus;
import cc.adabyte.blog.system.auth.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * JWT 认证过滤器。
 *
 * <p>拦截所有 /api/v2/** 请求。公开端点允许匿名访问；受保护端点必须提供有效 Bearer Token。
 * 若公开端点也携带了 Token，则同样会进行校验并将用户名写入 request attribute，供后续业务做细粒度授权。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String CURRENT_USERNAME_ATTR = AuthConstants.CURRENT_USERNAME_ATTRIBUTE;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 公开端点：匹配方法 + URI，按精确/正则分类
    private static final Pattern ARTICLE_DETAIL_PATTERN = Pattern.compile("^/api/v2/articles/\\d+$");
    private static final Pattern NOTE_DETAIL_PATTERN = Pattern.compile("^/api/v2/notes/\\d+$");
    private static final Pattern RESOURCE_DOWNLOAD_PATTERN = Pattern.compile("^/api/v2/resource/\\d+/content$");
    private static final Pattern MESSAGE_LIKE_PATTERN = Pattern.compile("^/api/v2/messages/\\d+/like$");
    private static final Pattern ARTICLE_BY_TAG_PATTERN = Pattern.compile("^/api/v2/article-tags/by-tag$");

    private final SysUserMapper sysUserMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

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

        // MCP 端点由 McpAuthInterceptor 通过 X-MCP-Key 独立鉴权，不走 JWT
        if (uri.startsWith("/mcp")) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isPublic = isPublicEndpoint(method, uri);
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            if (isPublic) {
                filterChain.doFilter(request, response);
                return;
            }
            log.warn("未提供认证令牌: {} {}", method, uri);
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已过期");
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            String username = JwtUtil.getUsername(token, jwtSecret);
            SysUser user = sysUserMapper.selectByUsername(username);
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                log.warn("用户不存在或已被禁用: {}", username);
                if (isPublic) {
                    filterChain.doFilter(request, response);
                    return;
                }
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已过期");
                return;
            }
            request.setAttribute(CURRENT_USERNAME_ATTR, username);
            request.setAttribute(AuthConstants.CURRENT_ROLE_ATTRIBUTE, user.getRole());
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("无效的 JWT 令牌: {}", e.getMessage());
            if (isPublic) {
                filterChain.doFilter(request, response);
                return;
            }
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已过期");
        }
    }

    // 与 GlobalExceptionHandler 保持同一错误契约：真实状态码 + Result JSON
    private void writeError(HttpServletResponse response, int status, String msg) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(Result.error(status, msg)));
    }

    private boolean isPublicEndpoint(String method, String uri) {
        if (!"GET".equals(method) && !"POST".equals(method)) {
            return false;
        }

        return switch (uri) {
            case "/api/v2/auth/login" -> "POST".equals(method);
            case "/api/v2/articles/published",
                 "/api/v2/articles/search",
                 "/api/v2/articles/archive",
                 "/api/v2/notes",
                 "/api/v2/tags",
                 "/api/v2/tags/suggest",
                 "/api/v2/config/site",
                 "/api/v2/resource/public",
                 "/api/v2/system/config/dashboard" -> "GET".equals(method);
            default -> {
                if ("GET".equals(method) && ARTICLE_DETAIL_PATTERN.matcher(uri).matches()) {
                    yield true;
                }
                if ("GET".equals(method) && NOTE_DETAIL_PATTERN.matcher(uri).matches()) {
                    yield true;
                }
                // 留言板：GET 列表与 POST 提交均对访客开放。
                // 该判定须放在 default 内按方法匹配，不能并入上方 GET-only case（switch 命中即终结）。
                if ("/api/v2/messages".equals(uri)) {
                    yield true;
                }
                // 访客点赞已发布留言（Service 层校验状态）
                if ("POST".equals(method) && MESSAGE_LIKE_PATTERN.matcher(uri).matches()) {
                    yield true;
                }
                // 资源下载允许匿名访问公开资源，非公开资源由 ResourceController 二次校验管理员身份
                if ("GET".equals(method) && RESOURCE_DOWNLOAD_PATTERN.matcher(uri).matches()) {
                    yield true;
                }
                // 访客按标签浏览已发布文章
                if ("GET".equals(method) && ARTICLE_BY_TAG_PATTERN.matcher(uri).matches()) {
                    yield true;
                }
                yield false;
            }
        };
    }
}
