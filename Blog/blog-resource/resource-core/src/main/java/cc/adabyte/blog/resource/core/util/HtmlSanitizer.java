package cc.adabyte.blog.resource.core.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jsoup.safety.Safelist;

/**
 * HTML 内容消毒器。
 *
 * <p>用于对返回给前端并通过 v-html 渲染的内容进行净化，移除 &lt;script&gt;、事件处理器等危险元素，
 * 同时保留博客/便签常用的安全标签和样式类。
 */
public final class HtmlSanitizer {

    private HtmlSanitizer() {
        // utility class
    }

    /**
     * 博客内容白名单：保留常见排版、代码块、图片、链接、表格等标签，
     * 并允许 class 属性以兼容 Vditor 等编辑器生成的样式类。
     */
    private static final Safelist CONTENT_SAFELIST = Safelist.relaxed()
            .addTags("del", "s", "strike", "u", "sub", "sup", "details", "summary")
            .addAttributes(":all", "class")
            .addAttributes("a", "target", "rel", "title")
            .addAttributes("img", "src", "alt", "title", "width", "height")
            .removeProtocols("a", "href", "ftp", "http", "https", "mailto")
            .removeProtocols("img", "src", "http", "https");

    /**
     * 对输入字符串进行消毒。
     *
     * <ul>
     *   <li>若输入为空或仅空白，原样返回；</li>
     *   <li>先按 HTML 解析并清理危险标签/属性；</li>
     *   <li>输出时保留原始结构，不强制补全 html/head/body。</li>
     * </ul>
     */
    public static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String cleaned = Jsoup.clean(raw, "", CONTENT_SAFELIST, new Document.OutputSettings()
                .prettyPrint(false)
                .charset(java.nio.charset.StandardCharsets.UTF_8)
                .escapeMode(Entities.EscapeMode.xhtml));
        return cleaned;
    }
}
