package cc.adabyte.blog.zoom.tag.service.impl;

import cc.adabyte.blog.zoom.tag.entity.Tag;
import cc.adabyte.blog.zoom.tag.mapper.TagMapper;
import cc.adabyte.blog.zoom.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    @Override
    public Tag create(Tag tag) {
        tagMapper.insert(tag);
        return tag;
    }

    @Override
    public List<Tag> suggestSimilar(String name) {
        return tagMapper.selectSimilar(name);
    }

    @Override
    public List<Tag> list() {
        return tagMapper.selectList(null);
    }

    @Override
    @Transactional
    public List<Tag> resolveByNames(List<String> names) {
        List<Tag> result = new ArrayList<>();
        if (names == null) return result;
        for (String raw : names) {
            if (raw == null || raw.isBlank()) continue;
            String name = raw.trim();
            Tag existing = tagMapper.selectByName(name);
            if (existing != null) {
                result.add(existing);
                continue;
            }
            Tag tag = new Tag();
            tag.setName(name);
            try {
                tagMapper.insert(tag);
            } catch (DuplicateKeyException e) {
                // 并发下同名已被插入，回查复用
                tag = tagMapper.selectByName(name);
            }
            if (tag != null) result.add(tag);
        }
        return result;
    }

    @Override
    public void deleteById(Long id) {
        tagMapper.deleteById(id);
    }
}
