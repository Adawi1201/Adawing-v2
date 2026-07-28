package cc.adabyte.blog.zoom.note.service.impl;

import cc.adabyte.blog.common.event.NoteDeletedEvent;
import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.zoom.note.entity.Note;
import cc.adabyte.blog.zoom.note.mapper.NoteMapper;
import cc.adabyte.blog.zoom.note.service.NoteService;
import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import cc.adabyte.blog.zoom.shared.enums.NoteType;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final NoteMapper noteMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PageResult<Note> listByType(NoteType type, int page, int size) {
        Page<Note> mpPage = new Page<>(page, size);
        int published = ContentStatus.PUBLISHED.getValue();
        var result = type != null
                ? noteMapper.selectByType(mpPage, type.getValue(), published)
                : noteMapper.selectAll(mpPage, published);
        return PageResult.of(result.getTotal(), result.getRecords(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public void save(Note note) {
        // 管理端直接发布路径：默认 PUBLISHED
        if (note.getStatus() == null) {
            note.setStatus(ContentStatus.PUBLISHED);
        }
        if (note.getId() == null) {
            noteMapper.insert(note);
        } else {
            noteMapper.updateById(note);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        noteMapper.deleteById(id);
        // 通知 system-review 清理关联的审核任务，避免删除后残留孤儿任务
        eventPublisher.publishEvent(new NoteDeletedEvent(id));
    }

    @Override
    public Note getById(Long id) {
        return noteMapper.selectById(id);
    }

    @Override
    @Transactional
    public Long saveDraft(Note note) {
        note.setId(null);
        note.setStatus(ContentStatus.DRAFT);
        if (note.getType() == null) {
            note.setType(NoteType.PERSONAL);
        }
        note.setCreateTime(LocalDateTime.now());
        note.setUpdateTime(LocalDateTime.now());
        noteMapper.insert(note);
        return note.getId();
    }

    @Override
    @Transactional
    public void submitForReview(Long id) {
        updateStatusIfPresent(id, ContentStatus.PENDING_REVIEW);
    }

    @Override
    @Transactional
    public void publishApproved(Long id) {
        updateStatusIfPresent(id, ContentStatus.PUBLISHED);
    }

    @Override
    @Transactional
    public void reject(Long id, String reason, String reviewerNote) {
        // note 不存拒绝原因（走 review_task），仅回退到草稿
        updateStatusIfPresent(id, ContentStatus.DRAFT);
        log.info("[Note] Rejected → DRAFT: noteId={}, reason={}", id, reason);
    }

    private void updateStatusIfPresent(Long id, ContentStatus status) {
        Note note = noteMapper.selectById(id);
        if (note != null) {
            note.setStatus(status);
            note.setUpdateTime(LocalDateTime.now());
            noteMapper.updateById(note);
        }
    }

    @Override
    public Note getPublishedById(Long id) {
        return noteMapper.selectPublishedById(id, ContentStatus.PUBLISHED.getValue());
    }

    @Override
    public Note getAdminById(Long id) {
        return noteMapper.selectAdminById(id);
    }

    @Override
    public List<Note> searchPublished(String keyword, int limit) {
        return noteMapper.selectByKeyword(keyword, ContentStatus.PUBLISHED.getValue(), limit);
    }
}
