package cc.adabyte.blog.boot;

import cc.adabyte.blog.common.model.SubmitReviewRequest;
import cc.adabyte.blog.system.review.service.ReviewService;
import cc.adabyte.blog.zoom.note.entity.Note;
import cc.adabyte.blog.zoom.note.service.NoteService;
import cc.adabyte.blog.zoom.shared.enums.NoteType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 动态删除清理审核任务回归测试。
 *
 * <p>此前 {@code NoteServiceImpl.delete} 只删 note 本身，不通知 system-review，
 * 导致删除已提交审核的动态后 review_task 残留成孤儿任务（文章走
 * ArticleDeletedEvent 有清理，动态漏了）。现动态删除发布 NoteDeletedEvent，
 * system-review 的 NoteDeletedEventListener 消费后清理对应审核任务。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DisplayName("动态删除清理审核任务测试")
class NoteDeleteReviewCleanupTest {

    @Autowired
    private NoteService noteService;
    @Autowired
    private ReviewService reviewService;

    @Test
    @DisplayName("删除动态时，其关联的审核任务一并被清理")
    void deletingNoteClearsReviewTask() {
        // 建一个草稿动态并提交审核，模拟 Agent 生成 → PENDING_REVIEW
        Note note = new Note();
        note.setTitle("待删除的审核动态");
        note.setContent("正文");
        note.setType(NoteType.TECH);
        Long noteId = noteService.saveDraft(note);
        noteService.submitForReview(noteId);

        SubmitReviewRequest req = new SubmitReviewRequest();
        req.setContentType("note");
        req.setContentId(noteId);
        req.setSubmitterType("agent");
        req.setSubmitterId("opencode");
        reviewService.submit(req);

        assertTrue(reviewService.findByContent("note", noteId).isPresent(),
                "前置条件：审核任务应已创建");

        // 删除动态 → 应级联清理审核任务
        noteService.delete(noteId);

        assertTrue(reviewService.findByContent("note", noteId).isEmpty(),
                "删除动态后审核任务应被清理，不得残留孤儿任务");
    }
}
