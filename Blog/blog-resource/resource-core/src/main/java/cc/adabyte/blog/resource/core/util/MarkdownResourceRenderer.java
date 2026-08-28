package cc.adabyte.blog.resource.core.util;

import cc.adabyte.blog.resource.core.entity.Resource;
import cc.adabyte.blog.resource.core.mapper.ResourceMapper;
import cc.adabyte.blog.resource.core.service.ResourceDirectUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 资源渲染器。
 *
 * <p>将 Markdown 中的 resource://id 占位符替换为实际下载 URL。
 *
 * <p>提供两种输出：
 * <ul>
 *   <li>{@link #render(String)}：替换后再做 HTML 消毒，用于访客输入等不可信内容（留言/便签），
 *       防止恶意脚本；</li>
 *   <li>{@link #renderWithoutSanitize(String)}：仅替换、不消毒，用于由前端 Vditor 渲染的
 *       Markdown 正文（文章）。HTML 消毒的 xhtml 转义会把公式中的 {@code < > &} 转成
 *       {@code &lt; &gt; &amp;}，导致 KaTeX 无法解析，故文章正文不走消毒——渲染安全由
 *       Vditor 负责。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class MarkdownResourceRenderer {

    private static final Pattern RESOURCE_PATTERN = Pattern.compile("resource://(\\d+)");

    private final ResourceMapper resourceMapper;
    private final ResourceDirectUrlResolver directUrlResolver;

    /**
     * 消毒路径**不发**预签名直链：{@link HtmlSanitizer} 使用 xhtml 转义，
     * 会把签名 URL 查询串里的 {@code &} 转成 {@code &amp;}，签名当场失效。
     * 留言 / 便签正文图片因此恒走代理转发。
     */
    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return markdown;
        }
        String replaced = replaceResourceUrls(markdown, false);
        return HtmlSanitizer.sanitize(replaced);
    }

    /**
     * 仅替换 resource:// 占位符，不做 HTML 消毒。
     * 用于交给前端 Vditor 渲染的 Markdown 正文，避免转义破坏 LaTeX 等语法。
     *
     * <p>因为不经消毒，此路径允许发出预签名直链（见
     * {@link ResourceDirectUrlResolver}）。
     */
    public String renderWithoutSanitize(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return markdown;
        }
        return replaceResourceUrls(markdown, true);
    }

    /**
     * @param allowDirect 是否允许把占位符替换成 OSS 预签名直链；
     *                    {@code false} 时恒用代理转发路径
     */
    private String replaceResourceUrls(String markdown, boolean allowDirect) {
        Matcher matcher = RESOURCE_PATTERN.matcher(markdown);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            Long resourceId = Long.valueOf(matcher.group(1));
            Resource resource = resourceMapper.selectById(resourceId);
            String replacement = resource != null
                ? resolveUrl(resource, resourceId, allowDirect)
                : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String resolveUrl(Resource resource, Long resourceId, boolean allowDirect) {
        if (allowDirect) {
            String direct = directUrlResolver.resolve(resource);
            if (direct != null) {
                return direct;
            }
        }
        return "/api/v2/resource/" + resourceId + "/content";
    }
}
