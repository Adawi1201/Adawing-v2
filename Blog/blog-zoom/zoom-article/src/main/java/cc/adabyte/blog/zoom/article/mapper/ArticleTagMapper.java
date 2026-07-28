package cc.adabyte.blog.zoom.article.mapper;

import cc.adabyte.blog.zoom.article.dto.ArticleTagRow;
import cc.adabyte.blog.zoom.article.entity.ArticleTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ArticleTagMapper extends BaseMapper<ArticleTag> {
    @Select("SELECT * FROM article_tag WHERE article_id = #{articleId}")
    List<ArticleTag> selectByArticleId(@Param("articleId") Long articleId);

    /** 批量取文章的标签（展示用），避免列表 N+1。 */
    @Select("""
        <script>
        SELECT at.article_id AS articleId, t.id AS tagId, t.name AS name, t.color AS color
        FROM article_tag at
        INNER JOIN tag t ON at.tag_id = t.id
        WHERE at.article_id IN
        <foreach item='id' collection='articleIds' open='(' separator=',' close=')'>#{id}</foreach>
        ORDER BY at.sort_order ASC, t.name ASC
        </script>
        """)
    List<ArticleTagRow> selectTagViewsByArticleIds(@Param("articleIds") List<Long> articleIds);

    @Delete("DELETE FROM article_tag WHERE article_id = #{articleId}")
    void deleteByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT COUNT(*) FROM article_tag WHERE tag_id = #{tagId}")
    Long countByTagId(@Param("tagId") Long tagId);

    /** 删除「文章已同时挂有目标标签」的源关联，避免迁移时撞唯一对约束。 */
    @Delete("""
        DELETE FROM article_tag
        WHERE tag_id = #{sourceTagId}
          AND article_id IN (SELECT article_id FROM (
              SELECT article_id FROM article_tag WHERE tag_id = #{targetTagId}
          ) t)
        """)
    void deleteConflictingSourceLinks(@Param("sourceTagId") Long sourceTagId,
                                      @Param("targetTagId") Long targetTagId);

    /** 将剩余源关联改指目标标签。 */
    @Update("UPDATE article_tag SET tag_id = #{targetTagId} WHERE tag_id = #{sourceTagId}")
    void repointRemaining(@Param("sourceTagId") Long sourceTagId,
                          @Param("targetTagId") Long targetTagId);
}
