package cc.adabyte.blog.resource.core.controller;

import cc.adabyte.blog.common.constants.ResourcePool;
import cc.adabyte.blog.common.exception.BusinessException;
import cc.adabyte.blog.common.result.Result;
import cc.adabyte.blog.resource.core.entity.Resource;
import cc.adabyte.blog.resource.core.service.ResourceDownload;
import cc.adabyte.blog.resource.core.service.ResourceService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.ContentDisposition;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/v2/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping("/upload")
    public Result<Resource> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "MISC") ResourcePool pool) {
        return Result.ok(resourceService.upload(file, pool));
    }

    @GetMapping("/{resourceId}/content")
    public void download(@PathVariable Long resourceId, HttpServletResponse response) {
        ResourceDownload download = resourceService.download(resourceId);
        response.setContentType(download.mimeType() != null ? download.mimeType() : "application/octet-stream");
        if (download.size() != null) {
            response.setContentLengthLong(download.size());
        }
        if (download.originalName() != null) {
            ContentDisposition cd = ContentDisposition.inline()
                    .filename(download.originalName(), StandardCharsets.UTF_8)
                    .build();
            response.setHeader("Content-Disposition", cd.toString());
        }
        try (InputStream in = download.stream();
             OutputStream out = response.getOutputStream()) {
            in.transferTo(out);
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
