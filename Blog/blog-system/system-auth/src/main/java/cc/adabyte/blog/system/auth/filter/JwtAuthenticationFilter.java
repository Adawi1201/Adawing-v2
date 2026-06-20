package cc.adabyte.blog.system.auth.filter;

import cc.adabyte.blog.common.util.JwtUtil;
import cc.adabyte.blog.system.auth.entity.SysUser;
import cc.adabyte.blog.system.auth.enums.UserStatus;
import cc.adabyte.blog.system.auth.mapper.SysUserMapper;
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
 * <p>拦截所有 /api/v2/** 请求（除显式配置的公开端点外），校验请求头中的 Bearer Token，
 * 并确认用户处于启用状态。校验通过后在 request attribute 中写入当前用户名，供后续业务使用。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String CURRENT_USERNAME_ATTR = "currentUsername";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    // 公开端点：匹配方法 + URI，按精确/正则分类
    private static final Pattern ARTICLE_DETAIL_PATTERN = Pattern.compile("^/api/v2/articles/\\d+$");
    private static final Pattern NOTE_DETAIL_PATTERN = Pattern.compile("^/api/v2/notes/\\d+$");
    private static final Pattern RESOURCE_DOWNLOAD_PATTERN = Pattern.compile("^/api/v2/resource/\\d+/content$");

    private final SysUserMapper sysUserMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(method) || isPublicEndpoint(method, uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("未提供认证令牌: {} {}", method, uri);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized");
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            String username = JwtUtil.getUsername(token, jwtSecret);
            SysUser user = sysUserMapper.selectByUsername(username);
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                log.warn("用户不存在或已被禁用: {}", username);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Unauthorized");
                return;
            }
            request.setAttribute(CURRENT_USERNAME_ATTR, username);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("无效的 JWT 令牌: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized");
        }
    }

    private boolean isPublicEndpoint(String method, String uri) {
        if (!"GET".equals(method) && !"POST".equals(method)) {
            return false;
        }

        return switch (uri) {
            case "/api/v2/auth/login" -> "POST".equals(method);
            case "/api/v2/articles/published",
                 "/api/v2/articles/archive",
                 "/api/v2/messages",
                 "/api/v2/notes",
                 "/api/v2/tags",
                 "/api/v2/tags/suggest",
                 "/api/v2/config/site" -> "GET".equals(method);
            default -> {
                if ("GET".equals(method) && ARTICLE_DETAIL_PATTERN.matcher(uri).matches()) {
                    yield true;
                }
                if ("GET".equals(method) && NOTE_DETAIL_PATTERN.matcher(uri).matches()) {
                    yield true;
                }
                if ("POST".equals(method) && "/api/v2/messages".equals(uri)) {
                    yield true;
                }
                // 资源下载在 P0-005 修复前保持公开，修复后将重新评估
                if ("GET".equals(method) && RESOURCE_DOWNLOAD_PATTERN.matcher(uri).matches()) {
                    yield true;
                }
                yield false;
            }
        };
    }
}
