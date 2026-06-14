package cc.adabyte.blog.zoom.tag.controller;

import cc.adabyte.blog.common.result.Result;
import cc.adabyte.blog.zoom.tag.entity.Tag;
import cc.adabyte.blog.zoom.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public Result<List<Tag>> list() {
        return Result.ok(tagService.listWithArticleCount());
    }

    @PostMapping
    public Result<Tag> create(@RequestBody Tag tag) {
        return Result.ok(tagService.create(tag));
    }

    @GetMapping("/suggest")
    public Result<List<Tag>> suggestSimilar(@RequestParam String name) {
        return Result.ok(tagService.suggestSimilar(name));
    }

    @PostMapping("/{sourceId}/merge/{targetId}")
    public Result<Void> merge(@PathVariable Long sourceId, @PathVariable Long targetId) {
        tagService.merge(sourceId, targetId);
        return Result.ok();
    }
}
