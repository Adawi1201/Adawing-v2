package cc.adabyte.blog.resource.core.service;

/**
 * 资源内容缓存值。持有完整字节，可被重复读取。
 * 不缓存 publicAccess：访问判定随引用计数变化，必须每次请求实时计算。
 */
public record CachedResource(
        byte[] content,
        String mimeType,
        Long size,
        String originalName
) {
}
