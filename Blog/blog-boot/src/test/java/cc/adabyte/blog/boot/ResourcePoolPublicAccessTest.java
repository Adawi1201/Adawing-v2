package cc.adabyte.blog.boot;

import cc.adabyte.blog.common.constants.ResourcePool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 资源池公开性契约测试。
 *
 * <p>约定：AVATAR / EMOJI 池默认对访客公开，ARTICLE / MISC 池需被引用后才公开
 * （避免草稿图外泄）。下载判定 {@code ACTIVE && (poolPublic || referenced)}
 * 依赖池的公开性分类。
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
