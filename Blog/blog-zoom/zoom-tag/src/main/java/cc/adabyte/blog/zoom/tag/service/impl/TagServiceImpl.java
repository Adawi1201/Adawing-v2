package cc.adabyte.blog.zoom.tag.service.impl;

import cc.adabyte.blog.zoom.tag.entity.Tag;
import cc.adabyte.blog.zoom.tag.mapper.TagMapper;
import cc.adabyte.blog.zoom.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    public List<Tag> listWithArticleCount() {
        return tagMapper.selectList(null);
    }

    @Override
    public void merge(Long sourceId, Long targetId) {
        throw new UnsupportedOperationException("待 ArticleTagMapper 就绪后实现");
    }

    @Override
    public void deleteIfUnused(Long id) {
        throw new UnsupportedOperationException("待 ArticleTagMapper 就绪后实现");
    }
}
