package cc.adabyte.blog.zoom.note.service;

import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.zoom.note.entity.Note;
import cc.adabyte.blog.zoom.shared.enums.NoteType;

import java.util.List;

public interface NoteService {
    PageResult<Note> listByType(NoteType type, int page, int size);
    void save(Note note);
    void delete(Long id);
    Note getById(Long id);

    // 审核流（对齐 article）
    Long saveDraft(Note note);
    void submitForReview(Long id);
    void publishApproved(Long id);
    void reject(Long id, String reason, String reviewerNote);
    Note getPublishedById(Long id);
    Note getAdminById(Long id);
    List<Note> searchPublished(String keyword, int limit);
}
