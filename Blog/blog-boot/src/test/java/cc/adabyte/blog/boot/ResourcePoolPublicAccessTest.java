package cc.adabyte.blog.boot;

import cc.adabyte.blog.common.constants.ResourcePool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 资源池公开性回归测试。
 *
 * <p>此前访客下载公开条件写死为 {@code ref_count > 0 && ACTIVE}，导致头像 / 图标 /
 * 站点头像等天然公开但不走引用计数的资源（AVATAR / EMOJI 池）被判为私有，匿名访客
 * 一律 404。修复后改由资源池的 {@code publicByDefault} 标志决定：AVATAR / EMOJI 默认
 * 公开，ARTICLE / MISC 仍需被引用后才对访客公开（避免草稿图外泄）。
 *
 * <p>本测试锁定池的公开性分类——下载判定 {@code ACTIVE && (poolPublic || referenced)}
 * 直接依赖它，改错分类会让本用例失败。
 */
@DisplayName("资源池公开性测试")
class ResourcePoolPublicAccessTest {

    @Test
    @DisplayName("AVATAR / EMOJI 池默认对访客公开")
    void publicPools() {
        assertTrue(ResourcePool.AVATAR.isPublicByDefault(),
                "AVATAR（站点头像/留言头像/links 图标）应默认公开");
        assertTrue(ResourcePool.EMOJI.isPublicByDefault(),
                "EMOJI（访客留言表情）应默认公开");
    }

    @Test
    @DisplayName("ARTICLE / MISC 池默认私有，需被引用后才公开")
    void privatePools() {
        assertFalse(ResourcePool.ARTICLE.isPublicByDefault(),
                "ARTICLE（文章/动态正文图片）不应默认公开，避免草稿图外泄");
        assertFalse(ResourcePool.MISC.isPublicByDefault(),
                "MISC（零散资源）不应默认公开");
    }
}
