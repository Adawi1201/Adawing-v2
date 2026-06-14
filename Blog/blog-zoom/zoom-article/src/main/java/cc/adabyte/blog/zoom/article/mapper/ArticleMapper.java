package cc.adabyte.blog.zoom.article.mapper;

import cc.adabyte.blog.zoom.article.entity.Article;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

public interface ArticleMapper extends BaseMapper<Article> {
    @Select("SELECT * FROM article WHERE status = #{status} AND is_hidden = 0 ORDER BY is_top DESC, create_time DESC")
    IPage<Article> selectPublished(Page<Article> page, @Param("status") int status);

    @Select("SELECT * FROM article ORDER BY create_time DESC")
    IPage<Article> selectAll(Page<Article> page);

    @Select("SELECT * FROM article WHERE id = #{id} LIMIT 1")
    Article selectAdminById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM article WHERE #{status} IS NULL OR status = #{status}")
    Long countByStatus(@Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM article WHERE source = #{source}")
    Long countBySource(@Param("source") int source);

    @Select("SELECT * FROM article WHERE status = #{status} AND is_hidden = 0 ORDER BY create_time DESC")
    List<Article> selectPublishedAll(@Param("status") int status);

    @Select("SELECT FORMATDATETIME(create_time, 'yyyy-MM') as month, COUNT(*) as cnt FROM article WHERE status = #{status} GROUP BY month ORDER BY month DESC")
    List<Map<String, Object>> selectArchiveStats(@Param("status") int status);

    @Select("""
        SELECT a.* FROM article a
        INNER JOIN article_tag at ON a.id = at.article_id
        INNER JOIN tag t ON at.tag_id = t.id
        WHERE a.status = #{status} AND a.is_hidden = 0 AND t.name = #{tagName}
        ORDER BY a.create_time DESC
        """)
    IPage<Article> selectByTag(Page<Article> page, @Param("tagName") String tagName,
                                @Param("status") int status);

    @Select("SELECT * FROM article WHERE id = #{id} AND status = #{status} AND is_hidden = 0 LIMIT 1")
    Article selectPublishedById(@Param("id") Long id, @Param("status") int status);

    @Update("UPDATE article SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    @Select("SELECT SUM(view_count) FROM article")
    Long selectTotalViewCount();

    @Select("""
        SELECT * FROM article
        WHERE (title LIKE CONCAT('%', #{keyword}, '%') OR summary LIKE CONCAT('%', #{keyword}, '%'))
        ORDER BY create_time DESC LIMIT #{limit}
        """)
    List<Article> selectByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);
}
