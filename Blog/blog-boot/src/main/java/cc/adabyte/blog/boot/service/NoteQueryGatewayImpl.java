package cc.adabyte.blog.boot.service;

import cc.adabyte.blog.common.gateway.NoteQueryGateway;
import cc.adabyte.blog.zoom.note.entity.Note;
import cc.adabyte.blog.zoom.note.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NoteQueryGatewayImpl implements NoteQueryGateway {

    private final NoteService noteService;

    @Override
    public Result getById(Long id) {
        Note note = noteService.getPublishedById(id);
        return note != null ? toResult(note) : null;
    }

    @Override
    public List<Result> search(String keyword, int limit) {
        return noteService.searchPublished(keyword, limit).stream()
                .map(this::toResult)
                .toList();
    }

    private Result toResult(Note note) {
        return new Result(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                note.getType() != null ? note.getType().name() : null,
                note.getSourceAgent()
        );
    }
}
