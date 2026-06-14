package cc.adabyte.blog.resource.core.controller;

import cc.adabyte.blog.common.constants.ResourcePool;
import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.common.result.Result;
import cc.adabyte.blog.resource.core.entity.Resource;
import cc.adabyte.blog.resource.core.service.ResourceAllocationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v2/admin/resources")
@RequiredArgsConstructor
public class AdminResourceController {

    private final ResourceAllocationFacade resourceFacade;

    @PostMapping("/upload")
    public Result<Resource> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "MISC") ResourcePool pool) {
        return Result.ok(resourceFacade.upload(file, pool));
    }

    @GetMapping
    public Result<PageResult<Resource>> page(
            @RequestParam(required = false) ResourcePool pool,
            @RequestParam(required = false, defaultValue = "false") boolean allowFallback,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (pool == null) {
            return Result.ok(resourceFacade.pageAll(page, size));
        }
        if (allowFallback) {
            return Result.ok(PageResult.of((long) 0, resourceFacade.listForUse(pool, true), (long) page, (long) size));
        }
        return Result.ok(resourceFacade.pageByPool(pool, page, size));
    }

    @PostMapping("/{id}/allocate")
    public Result<Resource> allocate(
            @PathVariable Long id,
            @RequestParam ResourcePool targetPool) {
        return Result.ok(resourceFacade.allocate(id, targetPool));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        resourceFacade.delete(id);
        return Result.ok();
    }
}
