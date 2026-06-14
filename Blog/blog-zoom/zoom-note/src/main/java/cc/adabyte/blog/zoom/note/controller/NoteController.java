package cc.adabyte.blog.zoom.note.controller;

import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.common.result.Result;
import cc.adabyte.blog.resource.core.service.ResourceAllocationFacade;
import cc.adabyte.blog.zoom.note.entity.Note;
import cc.adabyte.blog.zoom.note.service.NoteService;
import cc.adabyte.blog.zoom.shared.enums.NoteType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final ResourceAllocationFacade resourceFacade;

    @GetMapping
    public Result<PageResult<Note>> listByType(
            @RequestParam(required = false) NoteType type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResult<Note> result = noteService.listByType(type, page, size);
        result.getList().forEach(n -> n.setContent(renderContent(n.getContent())));
        return Result.ok(result);
    }

    @GetMapping("/{id}")
    public Result<Note> getById(@PathVariable Long id) {
        Note note = noteService.getById(id);
        if (note != null) {
            note.setContent(renderContent(note.getContent()));
        }
        return Result.ok(note);
    }

    @PostMapping
    public Result<Void> save(@RequestBody Note note) {
        noteService.save(note);
        return Result.ok();
    }

    private String renderContent(String markdown) {
        if (markdown == null) return null;
        return resourceFacade.renderMarkdown(markdown);
    }
}
