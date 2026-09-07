package cc.adabyte.blog.resource.core.controller;

import cc.adabyte.blog.common.constants.AuthConstants;
import cc.adabyte.blog.common.constants.ResourcePool;
import cc.adabyte.blog.common.exception.BusinessException;
import cc.adabyte.blog.common.result.Result;
import cc.adabyte.blog.resource.core.config.ResourceProxyProperties;
import cc.adabyte.blog.resource.core.entity.Resource;
import cc.adabyte.blog.resource.core.service.ResourceDirectUrlResolver;
import cc.adabyte.blog.resource.core.service.ResourceDownload;
import cc.adabyte.blog.resource.core.service.ResourcePoolService;
import cc.adabyte.blog.resource.core.service.ResourceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v2/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;
    private final ResourcePoolService resourcePoolService;
    private final ResourceDirectUrlResolver directUrlResolver;
    private final ResourceProxyProperties proxyProperties;

    /** 访客列举公开池资源（如留言板表情包）。仅允许 publicByDefault 的池，防止私有池被遍历。 */
    @GetMapping("/public")
    public Result<List<Resource>> listPublic(@RequestParam ResourcePool pool) {
        if (!pool.isPublicByDefault()) {
            throw BusinessException.forbidden("该资源池不对外开放");
        }
        return Result.ok(resourcePoolService.listForUse(pool, false));
    }

    @PostMapping("/upload")
    public Result<Resource> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "MISC") ResourcePool pool) {
        return Result.ok(resourceService.upload(file, pool));
    }

    /**
     * @param proxy 声明调用方只接受字节流、不接受跳转。无法跟随跨域重定向的调用方
     *              （XHR 取 blob，如管理端 {@code AuthImage}）必须带上。该参数同时
     *              使两类调用方落在不同的缓存键上。不放宽访问判定。
     */
    @GetMapping("/{resourceId}/content")
    public void download(@PathVariable Long resourceId,
                         @RequestParam(required = false, defaultValue = "false") boolean proxy,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        String currentUsername = (String) request.getAttribute(AuthConstants.CURRENT_USERNAME_ATTRIBUTE);

        // 匿名访问（访客端 <img>）在 DIRECT_SIGNED 模式下重定向到 OSS 预签名直链，
        // 不回源、不落缓存。直链仅在调用方能跟随跨域跳转时可用，故由 proxy 参数
        // 显式排除，不依赖对请求身份的推断。
        if (!proxy && currentUsername == null) {
            String directUrl = directUrlResolver.resolve(resourceId);
            if (directUrl != null) {
                response.setStatus(HttpServletResponse.SC_FOUND);
                response.setHeader("Location", directUrl);
                // 签名 URL 的剩余有效期恒 ≥ 一个 TTL 窗口，按此设置缓存不会缓存到失效链接
                response.setHeader("Cache-Control",
                        "private, max-age=" + proxyProperties.getSignTtlMinutes() * 60);
                return;
            }
        }

        ResourceDownload download = resourceService.download(resourceId);
        if (!download.publicAccess() && currentUsername == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        byte[] content = download.content();
        response.setContentType(download.mimeType() != null ? download.mimeType() : "application/octet-stream");
        response.setContentLengthLong(content.length);
        if (download.originalName() != null) {
            ContentDisposition cd = ContentDisposition.inline()
                    .filename(download.originalName(), StandardCharsets.UTF_8)
                    .build();
            response.setHeader("Content-Disposition", cd.toString());
        }
        try (OutputStream out = response.getOutputStream()) {
            out.write(content);
        } catch (IOException e) {
            log.error("资源下载失败: resourceId={}", resourceId, e);
            throw new BusinessException(500, "资源下载失败");
        }
    }

    @PostMapping("/{resourceId}/bind")
    public Result<Void> bind(
            @PathVariable Long resourceId,
            @RequestParam String module,
            @RequestParam Long objectId) {
        resourceService.bind(resourceId, module, objectId);
        return Result.ok();
    }

    @DeleteMapping("/{resourceId}")
    public Result<Void> delete(@PathVariable Long resourceId) {
        resourceService.physicalDelete(resourceId);
        return Result.ok();
    }
}
