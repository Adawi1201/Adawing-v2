package cc.adabyte.blog.zoom.note.service;

import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.zoom.note.entity.Note;
import cc.adabyte.blog.zoom.shared.enums.NoteType;

public interface NoteService {
    PageResult<Note> listByType(NoteType type, int page, int size);
    void save(Note note);
    void delete(Long id);
    Note getById(Long id);
}
