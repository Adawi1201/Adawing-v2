package cc.adabyte.blog.resource.core.util;

import cc.adabyte.blog.resource.core.entity.Resource;
import cc.adabyte.blog.resource.core.mapper.ResourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 资源渲染器。
 *
 * <p>将 Markdown 中的 resource://id 占位符替换为实际下载 URL，并对最终输出进行 HTML 消毒，
 * 防止前端通过 v-html 渲染时执行恶意脚本。
 */
@Component
@RequiredArgsConstructor
public class MarkdownResourceRenderer {

    private static final Pattern RESOURCE_PATTERN = Pattern.compile("resource://(\\d+)");

    private final ResourceMapper resourceMapper;

    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return markdown;
        }
        String replaced = replaceResourceUrls(markdown);
        return HtmlSanitizer.sanitize(replaced);
    }

    private String replaceResourceUrls(String markdown) {
        Matcher matcher = RESOURCE_PATTERN.matcher(markdown);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            Long resourceId = Long.valueOf(matcher.group(1));
            Resource resource = resourceMapper.selectById(resourceId);
            String replacement = resource != null
                ? "/api/v2/resource/" + resourceId + "/content"
                : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
