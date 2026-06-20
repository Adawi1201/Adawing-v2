package cc.adabyte.blog.common.constants;

/**
 * 认证相关常量。
 */
public final class AuthConstants {

    private AuthConstants() {
        // utility class
    }

    /**
     * JWT 认证过滤器写入 request attribute 的当前用户名 key。
     */
    public static final String CURRENT_USERNAME_ATTRIBUTE = "currentUsername";

    /**
     * JWT 认证过滤器写入 request attribute 的当前用户角色 key。
     */
    public static final String CURRENT_ROLE_ATTRIBUTE = "currentRole";
}
