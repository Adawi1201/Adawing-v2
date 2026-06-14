package cc.adabyte.blog.zoom.article.mapper;

import cc.adabyte.blog.zoom.article.entity.ArticleTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ArticleTagMapper extends BaseMapper<ArticleTag> {
    @Select("SELECT * FROM article_tag WHERE article_id = #{articleId}")
    List<ArticleTag> selectByArticleId(@Param("articleId") Long articleId);

    @Delete("DELETE FROM article_tag WHERE article_id = #{articleId}")
    void deleteByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT COUNT(*) FROM article_tag WHERE tag_id = #{tagId}")
    Long countByTagId(@Param("tagId") Long tagId);
}
