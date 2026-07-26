package cc.adabyte.blog.boot.service;

import cc.adabyte.blog.common.gateway.NoteDraftGateway;
import cc.adabyte.blog.common.model.SubmitReviewRequest;
import cc.adabyte.blog.system.review.service.ReviewService;
import cc.adabyte.blog.zoom.note.entity.Note;
import cc.adabyte.blog.zoom.note.service.NoteService;
import cc.adabyte.blog.zoom.shared.enums.NoteType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoteDraftGatewayImpl implements NoteDraftGateway {

    private final NoteService noteService;
    private final ReviewService reviewService;

    @Override
    public Long createNoteDraft(String title, String content, String type, String sourceAgent) {
        Note note = new Note();
        note.setTitle(title);
        note.setContent(content);
        note.setType(type != null ? NoteType.fromValue(type) : NoteType.PERSONAL);
        note.setSourceAgent(sourceAgent);

        Long noteId = noteService.saveDraft(note);
        log.info("[NoteDraftGateway] Created draft id={} title={}", noteId, title);

        // 自动提交审核链（AI 生成 note 永不自动发布）
        SubmitReviewRequest reviewReq = new SubmitReviewRequest();
        reviewReq.setContentType("note");
        reviewReq.setContentId(noteId);
        reviewReq.setSubmitterType("agent");
        reviewReq.setSubmitterId(sourceAgent);
        reviewService.submit(reviewReq);

        noteService.submitForReview(noteId);
        log.info("[NoteDraftGateway] Auto-submitted for review: noteId={}", noteId);

        return noteId;
    }
}
