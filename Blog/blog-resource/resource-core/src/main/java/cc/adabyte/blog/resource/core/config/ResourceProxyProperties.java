package cc.adabyte.blog.resource.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 资源访问模式配置。
 *
 * <p>控制图片资源是由后端代理转发（落 Caffeine 缓存，减少 OSS 调用），
 * 还是交给浏览器凭预签名 URL 直连 OSS（省服务器带宽与一次跳转）。
 *
 * <p>非图片资源不受本配置影响，恒走代理并落缓存。
 */
@Data
@Component
@ConfigurationProperties(prefix = "resource.proxy")
public class ResourceProxyProperties {

    public enum Mode {
        /** 代理转发：后端从 OSS 回源并落缓存，再把字节流返回给客户端。 */
        PROXY,
        /** 预签名直链：图片交由浏览器直连 OSS，要求 bucket 为私有读。 */
        DIRECT_SIGNED
    }

    /** 资源访问模式，默认代理转发。 */
    private Mode mode = Mode.PROXY;

    /**
     * 签名有效期（分钟），同时作为签名 URL 的稳定窗口长度。
     * 仅 {@link Mode#DIRECT_SIGNED} 生效，必须 ≥ 1。
     */
    private long signTtlMinutes = 30;
}
