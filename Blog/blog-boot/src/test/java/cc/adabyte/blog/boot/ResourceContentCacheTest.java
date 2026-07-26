package cc.adabyte.blog.boot;

import cc.adabyte.blog.resource.core.cache.ResourceCacheProperties;
import cc.adabyte.blog.resource.core.cache.ResourceContentCache;
import cc.adabyte.blog.resource.core.service.CachedResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 资源内容缓存回归测试。
 * 覆盖 Spec AC2/AC3/AC4 中可自动化的缓存类行为（不依赖 OSS）。
 */
@DisplayName("资源内容缓存测试")
class ResourceContentCacheTest {

    private ResourceContentCache newCache(long maxBytes) {
        ResourceCacheProperties props = new ResourceCacheProperties();
        props.setEnabled(true);
        props.setMaxSize(500);
        props.setExpireMinutes(30);
        props.setMaxBytes(maxBytes);
        return new ResourceContentCache(props);
    }

    private CachedResource resource(int bytes) {
        return new CachedResource(new byte[bytes], "image/png", (long) bytes, "x.png", true);
    }

    @Test
    @DisplayName("AC4 — put 后可命中，内容字节一致")
    void putThenHit() {
        ResourceContentCache cache = newCache(2 * 1024 * 1024);
        CachedResource r = resource(1024);
        cache.put(1L, r);

        CachedResource hit = cache.getIfPresent(1L);
        assertNotNull(hit, "应命中缓存");
        assertEquals(1024, hit.content().length);
        assertEquals("image/png", hit.mimeType());
        assertSame(r.content(), hit.content(), "字节数组应为同一引用（无拷贝）");
    }

    @Test
    @DisplayName("AC2 — 超过 maxBytes 的资源不缓存")
    void overSizeNotCached() {
        ResourceContentCache cache = newCache(1024); // 1KB 上限
        cache.put(2L, resource(2048));               // 2KB 超阈值

        assertNull(cache.getIfPresent(2L), "超阈值资源不应进缓存");
    }

    @Test
    @DisplayName("AC3 — invalidate 后不再命中")
    void invalidateEvicts() {
        ResourceContentCache cache = newCache(2 * 1024 * 1024);
        cache.put(3L, resource(512));
        assertNotNull(cache.getIfPresent(3L));

        cache.invalidate(3L);
        assertNull(cache.getIfPresent(3L), "失效后不应命中");
    }

    @Test
    @DisplayName("禁用时所有操作直穿，不缓存")
    void disabledIsNoop() {
        ResourceCacheProperties props = new ResourceCacheProperties();
        props.setEnabled(false);
        ResourceContentCache cache = new ResourceContentCache(props);

        cache.put(4L, resource(512));
        assertNull(cache.getIfPresent(4L), "禁用时不应缓存");
        cache.invalidate(4L); // 不应抛异常
    }
}
