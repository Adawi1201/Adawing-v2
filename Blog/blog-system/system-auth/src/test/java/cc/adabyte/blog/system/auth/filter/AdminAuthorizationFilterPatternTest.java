package cc.adabyte.blog.system.auth.filter;

import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AdminAuthorizationFilterPatternTest {

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Test
    void loginShouldNotMatchAdminPatterns() {
        String uri = "/api/v2/auth/login";
        String method = "POST";

        record AdminPattern(String pattern, Set<String> methods) {}
        List<AdminPattern> patterns = List.of(
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

        for (AdminPattern p : patterns) {
            boolean matched = matcher.match(p.pattern(), uri);
            if (matched && (p.methods() == null || p.methods().contains(method))) {
                fail("/api/v2/auth/login should not match admin pattern: " + p.pattern());
            }
        }
    }
}
