package cc.adabyte.blog.resource.core.service;

/**
 * 资源内容缓存值。持有完整字节，可被重复读取。
 */
public record CachedResource(
        byte[] content,
        String mimeType,
        Long size,
        String originalName,
        boolean publicAccess
) {
}
