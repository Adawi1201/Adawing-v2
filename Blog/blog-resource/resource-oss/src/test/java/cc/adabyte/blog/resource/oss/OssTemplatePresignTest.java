package cc.adabyte.blog.resource.oss;

import com.aliyun.oss.OSS;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 预签名直链测试。
 *
 * <p>核心不变量：同一 TTL 窗口内签出的 URL 必须完全相同，浏览器与 CDN 才能命中
 * 同一缓存对象，直链才能比代理更快。
 */
@DisplayName("OSS 预签名直链测试")
class OssTemplatePresignTest {

    private static final long TTL = 30;
    private static final long WINDOW = Duration.ofMinutes(TTL).toMillis();

    private OssProperties props() {
        OssProperties p = new OssProperties();
        p.setEndpoint("https://oss-cn.example.com");
        p.setAccessKey("ak");
        p.setSecretKey("sk");
        p.setBucketName("bucket");
        return p;
    }

    @Test
    @DisplayName("同一窗口内的不同时刻签出相同过期时间")
    void expiryIsStableWithinWindow() {
        long base = WINDOW * 100;

        assertEquals(OssTemplate.signExpiry(base, TTL),
                OssTemplate.signExpiry(base + 60_000, TTL),
                "同窗口内过期时间必须一致，签名 URL 才能保持稳定");
        assertEquals(OssTemplate.signExpiry(base, TTL),
                OssTemplate.signExpiry(base + WINDOW - 1, TTL),
                "窗口末尾仍应落在同一窗口");
    }

    @Test
    @DisplayName("跨窗口后过期时间前移一个窗口")
    void expiryAdvancesAcrossWindow() {
        long base = WINDOW * 100;

        Date current = OssTemplate.signExpiry(base, TTL);
        Date next = OssTemplate.signExpiry(base + WINDOW, TTL);

        assertNotEquals(current, next, "跨窗口必须换一个签名");
        assertEquals(WINDOW, next.getTime() - current.getTime(), "窗口步进应恰好为一个 TTL");
    }

    @Test
    @DisplayName("剩余有效期恒在 1~2 个 TTL 之间")
    void remainingValidityIsBetweenOneAndTwoWindows() {
        long[] moments = {WINDOW * 100, WINDOW * 100 + 1, WINDOW * 100 + WINDOW / 2, WINDOW * 101 - 1};

        for (long now : moments) {
            long remaining = OssTemplate.signExpiry(now, TTL).getTime() - now;
            assertTrue(remaining >= WINDOW,
                    "剩余有效期不得少于一个窗口，now=" + now + " remaining=" + remaining);
            assertTrue(remaining <= 2 * WINDOW,
                    "剩余有效期不应超过两个窗口，now=" + now + " remaining=" + remaining);
        }
    }

    @Test
    @DisplayName("presignedUrl 解析出 key 并委派给 OSS 客户端")
    void presignedUrlDelegatesWithParsedKey() throws MalformedURLException {
        OSS ossClient = mock(OSS.class);
        OssTemplate template = new OssTemplate(props(), ossClient);
        URL signed = new URL("https://bucket.oss-cn.example.com/article/x.png?Expires=1&Signature=sig");
        when(ossClient.generatePresignedUrl(anyString(), anyString(), any(Date.class))).thenReturn(signed);

        String result = template.presignedUrl("https://bucket.oss-cn.example.com/article/x.png", TTL);

        assertEquals(signed.toString(), result);
        ArgumentCaptor<Date> expiry = ArgumentCaptor.forClass(Date.class);
        verify(ossClient).generatePresignedUrl(eq("bucket"), eq("article/x.png"), expiry.capture());
        long remaining = expiry.getValue().getTime() - System.currentTimeMillis();
        assertTrue(remaining >= WINDOW, "签出的过期时间至少还有一个窗口，remaining=" + remaining);
    }

    @Test
    @DisplayName("OSS 未配置时抛 IllegalStateException")
    void presignedUrlFailsWhenUnconfigured() {
        OssTemplate template = new OssTemplate(props(), null);

        assertThrows(IllegalStateException.class,
                () -> template.presignedUrl("https://bucket.oss-cn.example.com/article/x.png", TTL));
    }
}
