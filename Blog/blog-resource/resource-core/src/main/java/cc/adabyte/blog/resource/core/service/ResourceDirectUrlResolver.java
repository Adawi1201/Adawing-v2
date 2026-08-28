package cc.adabyte.blog.resource.core.service;

import cc.adabyte.blog.common.constants.ResourceStatus;
import cc.adabyte.blog.resource.core.config.ResourceProxyProperties;
import cc.adabyte.blog.resource.core.entity.Resource;
import cc.adabyte.blog.resource.core.mapper.ResourceMapper;
import cc.adabyte.blog.resource.oss.OssTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 图片资源直链判定。
 *
 * <p>{@link ResourceProxyProperties.Mode#DIRECT_SIGNED} 模式下为符合条件的图片签发
 * OSS 预签名 URL，让浏览器直连对象存储；不符合条件时返回 {@code null}，
 * 由调用方回落到原有的代理转发路径。
 *
 * <p>直链条件刻意保守：
 * <ul>
 *   <li>仅图片——非图片资源恒走代理并落缓存，缓存策略不受本开关影响；</li>
 *   <li>仅通过访问判定的资源（{@code ACTIVE} 且所在池默认公开或已被引用）——
 *       未引用资源的 URL 不外发，避免草稿图随直链泄漏；</li>
 *   <li>签名失败时降级而非抛出——OSS 配置缺失应该退回代理，而不是让全站图片变空白。</li>
 * </ul>
 */
@Slf4j
@Component
public class ResourceDirectUrlResolver {

    private final ResourceMapper resourceMapper;
    private final OssTemplate ossTemplate;
    private final ResourceProxyProperties props;

    public ResourceDirectUrlResolver(ResourceMapper resourceMapper,
                                     OssTemplate ossTemplate,
                                     ResourceProxyProperties props) {
        if (props.getMode() == ResourceProxyProperties.Mode.DIRECT_SIGNED && props.getSignTtlMinutes() < 1) {
            throw new IllegalStateException(
                    "resource.proxy.sign-ttl-minutes 必须 >= 1，当前值: " + props.getSignTtlMinutes());
        }
        this.resourceMapper = resourceMapper;
        this.ossTemplate = ossTemplate;
        this.props = props;
    }

    /** 按 ID 查库后判定。返回 null 表示应走代理转发。 */
    public String resolve(Long resourceId) {
        if (resourceId == null || props.getMode() != ResourceProxyProperties.Mode.DIRECT_SIGNED) {
            return null;
        }
        return resolve(resourceMapper.selectById(resourceId));
    }

    /** 复用已加载的实体判定，避免重复查库。返回 null 表示应走代理转发。 */
    public String resolve(Resource resource) {
        if (props.getMode() != ResourceProxyProperties.Mode.DIRECT_SIGNED) {
            return null;
        }
        if (resource == null || !isImage(resource) || !isPublicAccess(resource)) {
            return null;
        }
        try {
            return ossTemplate.presignedUrl(resource.getUrl(), props.getSignTtlMinutes());
        } catch (RuntimeException e) {
            log.warn("[Resource] 预签名失败，回落代理转发: id={} url={}", resource.getId(), resource.getUrl(), e);
            return null;
        }
    }

    private boolean isImage(Resource resource) {
        return resource.getMimeType() != null && resource.getMimeType().startsWith("image/");
    }

    /** 与 ResourceServiceImpl#download 的访问判定保持一致。 */
    private boolean isPublicAccess(Resource resource) {
        if (resource.getStatus() != ResourceStatus.ACTIVE) {
            return false;
        }
        boolean poolPublic = resource.getPool() != null && resource.getPool().isPublicByDefault();
        boolean referenced = resource.getRefCount() != null && resource.getRefCount() > 0;
        return poolPublic || referenced;
    }
}
