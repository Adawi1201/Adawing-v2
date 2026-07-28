package cc.adabyte.blog.system.review.event;

import cc.adabyte.blog.common.event.NoteDeletedEvent;
import cc.adabyte.blog.system.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NoteDeletedEventListener {

    private final ReviewService reviewService;

    @EventListener
    public void onNoteDeleted(NoteDeletedEvent event) {
        reviewService.deleteByContent("note", event.noteId());
    }
}
