package cc.adabyte.blog.resource.core.controller;

import cc.adabyte.blog.common.constants.AuthConstants;
import cc.adabyte.blog.common.constants.ResourcePool;
import cc.adabyte.blog.common.exception.BusinessException;
import cc.adabyte.blog.common.result.Result;
import cc.adabyte.blog.resource.core.entity.Resource;
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

    /** 访客列举公开池资源（如留言板表情包）。仅允许 publicByDefault 的池，防止私有池被遍历。 */
    @GetMapping("/public")
    public Result<List<Resource>> listPublic(@RequestParam ResourcePool pool) {
        if (!pool.isPublicByDefault()) {
            throw new BusinessException("该资源池不对外开放");
        }
        return Result.ok(resourcePoolService.listForUse(pool, false));
    }

    @PostMapping("/upload")
    public Result<Resource> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "MISC") ResourcePool pool) {
        return Result.ok(resourceService.upload(file, pool));
    }

    @GetMapping("/{resourceId}/content")
    public void download(@PathVariable Long resourceId,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        ResourceDownload download = resourceService.download(resourceId);
        if (!download.publicAccess()) {
            String currentUsername = (String) request.getAttribute(AuthConstants.CURRENT_USERNAME_ATTRIBUTE);
            if (currentUsername == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
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
            throw new BusinessException("资源下载失败");
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
