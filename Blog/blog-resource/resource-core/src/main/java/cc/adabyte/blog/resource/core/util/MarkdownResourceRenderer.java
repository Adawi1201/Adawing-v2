package cc.adabyte.blog.resource.core.util;

import cc.adabyte.blog.resource.core.entity.Resource;
import cc.adabyte.blog.resource.core.mapper.ResourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class MarkdownResourceRenderer {

    private static final Pattern RESOURCE_PATTERN = Pattern.compile("resource://(\\d+)");

    private final ResourceMapper resourceMapper;

    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return markdown;
        }
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
