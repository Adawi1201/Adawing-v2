package cc.adabyte.blog.resource.core.cache;

import cc.adabyte.blog.resource.core.service.CachedResource;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 资源二进制内容缓存。命中时跳过 DB 查询与 OSS 回源。
 * 仅缓存字节数不超过 {@link ResourceCacheProperties#getMaxBytes()} 的资源。
 */
@Slf4j
@Component
public class ResourceContentCache {

    private final ResourceCacheProperties props;
    private final Cache<Long, CachedResource> cache;

    public ResourceContentCache(ResourceCacheProperties props) {
        this.props = props;
        if (props.isEnabled()) {
            this.cache = Caffeine.newBuilder()
                    .maximumSize(props.getMaxSize())
                    .expireAfterAccess(Duration.ofMinutes(props.getExpireMinutes()))
                    .build();
            log.info("[ResourceCache] 已启用: maxSize={} expireMinutes={} maxBytes={}",
                    props.getMaxSize(), props.getExpireMinutes(), props.getMaxBytes());
        } else {
            this.cache = null;
            log.info("[ResourceCache] 已禁用");
        }
    }

    public CachedResource getIfPresent(Long id) {
        return cache == null ? null : cache.getIfPresent(id);
    }

    public void put(Long id, CachedResource resource) {
        if (cache == null || resource == null || resource.content() == null) {
            return;
        }
        if (resource.content().length > props.getMaxBytes()) {
            return; // 超阈值不缓存
        }
        cache.put(id, resource);
    }

    public void invalidate(Long id) {
        if (cache != null) {
            cache.invalidate(id);
        }
    }
}
