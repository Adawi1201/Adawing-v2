package cc.adabyte.blog.zoom.tag.service;

import cc.adabyte.blog.zoom.tag.entity.Tag;

import java.util.List;

public interface TagService {
    Tag create(Tag tag);
    List<Tag> suggestSimilar(String name);
    List<Tag> listWithArticleCount();
    void merge(Long sourceId, Long targetId);
    void deleteIfUnused(Long id);
}
