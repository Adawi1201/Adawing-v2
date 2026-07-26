package cc.adabyte.blog.resource.core.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "resource.cache")
public class ResourceCacheProperties {
    /** 是否启用资源内容缓存 */
    private boolean enabled = true;
    /** 最大缓存条数 */
    private long maxSize = 500;
    /** 访问后过期时间（分钟） */
    private long expireMinutes = 30;
    /** 单条缓存字节上限，超过则不缓存 */
    private long maxBytes = 2 * 1024 * 1024;
}
