package cc.adabyte.blog.resource.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Date;

@Slf4j
@Component
public class OssTemplate {

    private final OSS ossClient;
    private final OssProperties props;

    @Autowired
    public OssTemplate(OssProperties props) {
        this.props = props;
        if (!isConfigured(props)) {
            log.warn("OSS 配置不完整（endpoint/accessKey/secretKey/bucketName 存在空值），OssTemplate 将以降级模式运行，上传/下载 OSS 操作将抛出异常");
            this.ossClient = null;
        } else {
            this.ossClient = new OSSClientBuilder().build(props.getEndpoint(), props.getAccessKey(), props.getSecretKey());
        }
    }

    OssTemplate(OssProperties props, OSS ossClient) {
        this.props = props;
        this.ossClient = ossClient;
    }

    private static boolean isConfigured(OssProperties props) {
        return StringUtils.hasText(props.getEndpoint())
                && StringUtils.hasText(props.getAccessKey())
                && StringUtils.hasText(props.getSecretKey())
                && StringUtils.hasText(props.getBucketName());
    }

    private void ensureClient() {
        if (ossClient == null) {
            throw new IllegalStateException("OSS 未配置，无法执行对象存储操作");
        }
    }

    public void upload(String key, byte[] content, String contentType) {
        ensureClient();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(content.length);
        ossClient.putObject(props.getBucketName(), key, new ByteArrayInputStream(content), metadata);
    }

    public void delete(String key) {
        ensureClient();
        ossClient.deleteObject(props.getBucketName(), key);
    }

    public void deleteByUrl(String url) {
        ensureClient();
        String key = parseKey(url);
        ossClient.deleteObject(props.getBucketName(), key);
    }

    public String getUrl(String key) {
        String endpoint = props.getEndpoint();
        String protocol = endpoint.startsWith("https://") ? "https://" : "http://";
        String host = endpoint.replaceFirst("^https?://", "");
        if (host.startsWith(props.getBucketName() + ".")) {
            return endpoint + "/" + key;
        }
        return protocol + props.getBucketName() + "." + host + "/" + key;
    }

    /**
     * 计算预签名 URL 的过期时刻，对齐到 ttl 长度的固定窗口。
     *
     * <p>过期时刻落在窗口边界（{@code next + 2} 个窗口），使同一窗口内所有请求
     * 签出完全相同的 URL，浏览器与 CDN 可复用同一缓存对象；窗口边缘取到的
     * URL 至少还有一个完整窗口有效，实际有效期在 1~2 个窗口之间浮动。
     */
    static Date signExpiry(long nowMillis, long ttlMinutes) {
        long window = Duration.ofMinutes(ttlMinutes).toMillis();
        return new Date(((nowMillis / window) + 2) * window);
    }

    /**
     * 为已存储的资源 URL 签发预签名直链，供浏览器直连私有读 bucket。
     *
     * @param url         入库时由 {@link #getUrl(String)} 生成的 OSS URL
     * @param ttlMinutes  签名有效期（分钟），同时是签名 URL 的稳定窗口
     */
    public String presignedUrl(String url, long ttlMinutes) {
        ensureClient();
        String key = parseKey(url);
        return ossClient.generatePresignedUrl(props.getBucketName(), key,
                signExpiry(System.currentTimeMillis(), ttlMinutes)).toString();
    }

    public InputStream download(String url) {
        ensureClient();
        String key = parseKey(url);
        OSSObject object = ossClient.getObject(props.getBucketName(), key);
        return object.getObjectContent();
    }

    String parseKey(String url) {
        String endpoint = props.getEndpoint();
        if (url.startsWith(endpoint + "/")) {
            return url.substring(endpoint.length() + 1);
        }
        String protocol = endpoint.startsWith("https://") ? "https://" : "http://";
        String host = endpoint.replaceFirst("^https?://", "");
        String bucketHost = protocol + props.getBucketName() + "." + host;
        if (url.startsWith(bucketHost + "/")) {
            return url.substring(bucketHost.length() + 1);
        }
        throw new IllegalArgumentException("无法从 URL 解析 OSS key: " + url);
    }
}
