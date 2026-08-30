package cc.adabyte.blog.boot;

import cc.adabyte.blog.common.constants.AuthConstants;
import cc.adabyte.blog.common.constants.ResourcePool;
import cc.adabyte.blog.common.constants.ResourceStatus;
import cc.adabyte.blog.resource.core.config.ResourceProxyProperties;
import cc.adabyte.blog.resource.core.controller.ResourceController;
import cc.adabyte.blog.resource.core.entity.Resource;
import cc.adabyte.blog.resource.core.mapper.ResourceMapper;
import cc.adabyte.blog.resource.core.service.ResourceDirectUrlResolver;
import cc.adabyte.blog.resource.core.service.ResourceDownload;
import cc.adabyte.blog.resource.core.service.ResourcePoolService;
import cc.adabyte.blog.resource.core.service.ResourceService;
import cc.adabyte.blog.resource.core.util.HtmlSanitizer;
import cc.adabyte.blog.resource.core.util.MarkdownResourceRenderer;
import cc.adabyte.blog.resource.oss.OssTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 资源访问模式（PROXY / DIRECT_SIGNED）测试。
 *
 * <p>覆盖的关键契约：
 * <ul>
 *   <li>PROXY 模式恒走代理回源，不签发签名直链；</li>
 *   <li>直链只发给「图片 + 通过访问判定」的资源，未引用资源与非图片资源恒走代理；</li>
 *   <li>消毒渲染路径恒走代理——HTML 转义会破坏签名；</li>
 *   <li>签名失败时降级为代理，不让页面图片变空白。</li>
 * </ul>
 */
@DisplayName("资源访问模式测试")
class ResourceProxyModeTest {

    private static final String SIGNED_URL =
            "https://bucket.oss-cn.example.com/article/x.png?Expires=1&Signature=sig";
    private static final String PROXY_URL = "/api/v2/resource/7/content";
    private static final String MARKDOWN = "![img](resource://7)";

    private ResourceMapper resourceMapper;
    private OssTemplate ossTemplate;

    @BeforeEach
    void setUp() {
        resourceMapper = mock(ResourceMapper.class);
        ossTemplate = mock(OssTemplate.class);
    }

    private ResourceProxyProperties props(ResourceProxyProperties.Mode mode) {
        ResourceProxyProperties p = new ResourceProxyProperties();
        p.setMode(mode);
        return p;
    }

    private MarkdownResourceRenderer renderer(ResourceProxyProperties props) {
        return new MarkdownResourceRenderer(resourceMapper,
                new ResourceDirectUrlResolver(resourceMapper, ossTemplate, props));
    }

    private Resource resource(ResourcePool pool, int refCount, String mimeType, ResourceStatus status) {
        Resource r = new Resource();
        r.setId(7L);
        r.setPool(pool);
        r.setStatus(status);
        r.setRefCount(refCount);
        r.setMimeType(mimeType);
        r.setUrl("https://bucket.oss-cn.example.com/article/x.png");
        r.setOriginalName("x.png");
        r.setSize(4L);
        return r;
    }

    private Resource publicImage() {
        return resource(ResourcePool.ARTICLE, 1, "image/png", ResourceStatus.ACTIVE);
    }

    private void stubResource(Resource resource) {
        when(resourceMapper.selectById(7L)).thenReturn(resource);
    }

    private void stubSigning() {
        when(ossTemplate.presignedUrl(anyString(), anyLong())).thenReturn(SIGNED_URL);
    }

    @Nested
    @DisplayName("PROXY 模式")
    class ProxyMode {

        @Test
        @DisplayName("公开图片仍替换为代理路径，且不触碰 OSS 签名")
        void publicImageKeepsProxyUrl() {
            stubResource(publicImage());

            String out = renderer(props(ResourceProxyProperties.Mode.PROXY)).renderWithoutSanitize(MARKDOWN);

            assertTrue(out.contains(PROXY_URL), "PROXY 模式必须维持代理路径，实际: " + out);
            verifyNoInteractions(ossTemplate);
        }
    }

    @Nested
    @DisplayName("DIRECT_SIGNED 模式")
    class DirectSignedMode {

        private MarkdownResourceRenderer renderer;

        @BeforeEach
        void init() {
            renderer = renderer(props(ResourceProxyProperties.Mode.DIRECT_SIGNED));
        }

        @Test
        @DisplayName("已引用的图片替换为签名直链")
        void referencedImageGetsSignedUrl() {
            stubResource(publicImage());
            stubSigning();

            String out = renderer.renderWithoutSanitize(MARKDOWN);

            assertTrue(out.contains(SIGNED_URL), "应输出签名直链，实际: " + out);
            assertFalse(out.contains(PROXY_URL), "不应再包含代理路径");
        }

        @Test
        @DisplayName("未引用的私有池图片回落代理路径（草稿图不外泄）")
        void unreferencedImageFallsBackToProxy() {
            stubResource(resource(ResourcePool.ARTICLE, 0, "image/png", ResourceStatus.ACTIVE));

            String out = renderer.renderWithoutSanitize(MARKDOWN);

            assertTrue(out.contains(PROXY_URL), "未引用资源必须回落代理，实际: " + out);
            verifyNoInteractions(ossTemplate);
        }

        @Test
        @DisplayName("默认公开池图片免引用即可直链")
        void publicPoolImageGetsSignedUrlWithoutReference() {
            stubResource(resource(ResourcePool.AVATAR, 0, "image/png", ResourceStatus.ACTIVE));
            stubSigning();

            String out = renderer.renderWithoutSanitize(MARKDOWN);

            assertTrue(out.contains(SIGNED_URL), "AVATAR 池默认公开，应直链，实际: " + out);
        }

        @Test
        @DisplayName("非图片资源回落代理路径（缓存策略不受影响）")
        void nonImageFallsBackToProxy() {
            stubResource(resource(ResourcePool.ARTICLE, 1, "application/pdf", ResourceStatus.ACTIVE));

            String out = renderer.renderWithoutSanitize(MARKDOWN);

            assertTrue(out.contains(PROXY_URL), "非图片必须回落代理，实际: " + out);
            verifyNoInteractions(ossTemplate);
        }

        @Test
        @DisplayName("非 ACTIVE 资源回落代理路径")
        void nonActiveFallsBackToProxy() {
            stubResource(resource(ResourcePool.ARTICLE, 1, "image/png", ResourceStatus.ORPHAN));

            String out = renderer.renderWithoutSanitize(MARKDOWN);

            assertTrue(out.contains(PROXY_URL), "非 ACTIVE 必须回落代理，实际: " + out);
            verifyNoInteractions(ossTemplate);
        }

        @Test
        @DisplayName("消毒渲染路径恒走代理，不发签名 URL")
        void sanitizedRenderNeverEmitsSignedUrl() {
            stubResource(publicImage());

            String out = renderer.render(MARKDOWN);

            assertTrue(out.contains(PROXY_URL), "留言/便签正文必须走代理，实际: " + out);
            assertFalse(out.contains("Signature"), "消毒路径不得出现签名参数");
            verifyNoInteractions(ossTemplate);
        }

        @Test
        @DisplayName("签名失败时降级为代理路径，不抛异常")
        void signingFailureFallsBackToProxy() {
            stubResource(publicImage());
            when(ossTemplate.presignedUrl(anyString(), anyLong()))
                    .thenThrow(new IllegalStateException("OSS 未配置，无法执行对象存储操作"));

            String out = assertDoesNotThrow(() -> renderer.renderWithoutSanitize(MARKDOWN));

            assertTrue(out.contains(PROXY_URL), "签名失败应回落代理，实际: " + out);
        }
    }

    @Nested
    @DisplayName("下载端点")
    class DownloadEndpoint {

        private ResourceService resourceService;

        private MockMvc mockMvc(ResourceProxyProperties props) {
            resourceService = mock(ResourceService.class);
            ResourceController controller = new ResourceController(resourceService,
                    mock(ResourcePoolService.class),
                    new ResourceDirectUrlResolver(resourceMapper, ossTemplate, props),
                    props);
            return MockMvcBuilders.standaloneSetup(controller).build();
        }

        private void stubDownload(boolean publicAccess, String mimeType) {
            when(resourceService.download(7L)).thenReturn(
                    new ResourceDownload(new byte[]{1, 2, 3, 4}, mimeType, 4L, "x.png", publicAccess));
        }

        @Test
        @DisplayName("匿名 + 公开图片 + DIRECT_SIGNED：302 到签名直链，不回源")
        void anonymousPublicImageRedirects() throws Exception {
            MockMvc mvc = mockMvc(props(ResourceProxyProperties.Mode.DIRECT_SIGNED));
            stubResource(publicImage());
            stubSigning();

            mvc.perform(get("/api/v2/resource/7/content"))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", SIGNED_URL))
                    .andExpect(header().string("Cache-Control", "private, max-age=1800"));

            verifyNoInteractions(resourceService);
        }

        @Test
        @DisplayName("匿名 + 公开图片 + PROXY：200 字节流")
        void anonymousPublicImageProxies() throws Exception {
            MockMvc mvc = mockMvc(props(ResourceProxyProperties.Mode.PROXY));
            stubResource(publicImage());
            stubDownload(true, "image/png");

            mvc.perform(get("/api/v2/resource/7/content"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "image/png"));

            verify(resourceService).download(7L);
            verifyNoInteractions(ossTemplate);
        }

        @Test
        @DisplayName("已登录 + DIRECT_SIGNED：仍走代理字节流（管理端 AuthImage 不受影响）")
        void authenticatedRequestAlwaysProxies() throws Exception {
            MockMvc mvc = mockMvc(props(ResourceProxyProperties.Mode.DIRECT_SIGNED));
            stubResource(publicImage());
            stubDownload(true, "image/png");

            mvc.perform(get("/api/v2/resource/7/content")
                            .requestAttr(AuthConstants.CURRENT_USERNAME_ATTRIBUTE, "admin"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "image/png"));

            verify(resourceService).download(7L);
            verifyNoInteractions(ossTemplate);
        }

        @Test
        @DisplayName("匿名 + 未引用图片 + DIRECT_SIGNED：仍是 404")
        void anonymousUnreferencedImageStays404() throws Exception {
            MockMvc mvc = mockMvc(props(ResourceProxyProperties.Mode.DIRECT_SIGNED));
            stubResource(resource(ResourcePool.ARTICLE, 0, "image/png", ResourceStatus.ACTIVE));
            stubDownload(false, "image/png");

            mvc.perform(get("/api/v2/resource/7/content"))
                    .andExpect(status().isNotFound());

            verifyNoInteractions(ossTemplate);
        }

        @Test
        @DisplayName("匿名 + 非图片 + DIRECT_SIGNED：200 字节流（缓存路径不变）")
        void anonymousNonImageProxies() throws Exception {
            MockMvc mvc = mockMvc(props(ResourceProxyProperties.Mode.DIRECT_SIGNED));
            stubResource(resource(ResourcePool.ARTICLE, 1, "application/pdf", ResourceStatus.ACTIVE));
            stubDownload(true, "application/pdf");

            mvc.perform(get("/api/v2/resource/7/content"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "application/pdf"));

            verify(resourceService).download(7L);
            verifyNoInteractions(ossTemplate);
        }
    }

    @Test
    @DisplayName("HTML 消毒会转义 & —— 这是消毒路径不能发签名 URL 的原因")
    void sanitizerBreaksSignedQueryString() {
        String sanitized = HtmlSanitizer.sanitize("![img](https://host/x.png?Expires=1&Signature=sig)");

        assertTrue(sanitized.contains("&amp;"),
                "消毒把 & 转成 &amp;，签名随即失效，实际: " + sanitized);
    }

    @Test
    @DisplayName("DIRECT_SIGNED 下非法 TTL 启动即失败")
    void invalidTtlFailsFast() {
        ResourceProxyProperties props = props(ResourceProxyProperties.Mode.DIRECT_SIGNED);
        props.setSignTtlMinutes(0);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new ResourceDirectUrlResolver(resourceMapper, ossTemplate, props));
        assertTrue(e.getMessage().contains("sign-ttl-minutes"), "错误信息应指向具体配置项");
    }
}
