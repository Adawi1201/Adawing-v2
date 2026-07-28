package cc.adabyte.blog.zoom.tag.service;

import cc.adabyte.blog.zoom.tag.entity.Tag;

import java.util.List;

public interface TagService {
    Tag create(Tag tag);
    List<Tag> suggestSimilar(String name);
    List<Tag> list();

    /**
     * 按名称解析标签：已存在则返回，不存在则创建。用于文章保存时落地标签词条。
     * 返回带 id 的持久化标签，供调用方写入 article_tag。
     */
    List<Tag> resolveByNames(List<String> names);

    /** 按 id 删除标签词条（引用校验由 article 域负责，此处仅删词表）。 */
    void deleteById(Long id);
}
