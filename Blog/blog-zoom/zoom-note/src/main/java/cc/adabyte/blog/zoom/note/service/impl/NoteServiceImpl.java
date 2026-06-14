package cc.adabyte.blog.zoom.note.service.impl;

import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.zoom.note.entity.Note;
import cc.adabyte.blog.zoom.note.mapper.NoteMapper;
import cc.adabyte.blog.zoom.note.service.NoteService;
import cc.adabyte.blog.zoom.shared.enums.NoteType;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final NoteMapper noteMapper;

    @Override
    public PageResult<Note> listByType(NoteType type, int page, int size) {
        Page<Note> mpPage = new Page<>(page, size);
        var result = type != null
                ? noteMapper.selectByType(mpPage, type.getValue())
                : noteMapper.selectAll(mpPage);
        return PageResult.of(result.getTotal(), result.getRecords(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public void save(Note note) {
        if (note.getId() == null) {
            noteMapper.insert(note);
        } else {
            noteMapper.updateById(note);
        }
    }

    @Override
    public Note getById(Long id) {
        return noteMapper.selectById(id);
    }
}
