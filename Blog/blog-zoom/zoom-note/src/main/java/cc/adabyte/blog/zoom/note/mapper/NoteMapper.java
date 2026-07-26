package cc.adabyte.blog.zoom.note.mapper;

import cc.adabyte.blog.zoom.note.entity.Note;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface NoteMapper extends BaseMapper<Note> {
    @Select("SELECT * FROM note WHERE type = #{type} AND status = #{status} ORDER BY create_time DESC")
    IPage<Note> selectByType(Page<Note> page, @Param("type") int type, @Param("status") int status);

    @Select("SELECT * FROM note WHERE status = #{status} ORDER BY create_time DESC")
    IPage<Note> selectAll(Page<Note> page, @Param("status") int status);

    @Select("SELECT * FROM note WHERE id = #{id} AND status = #{status} LIMIT 1")
    Note selectPublishedById(@Param("id") Long id, @Param("status") int status);

    @Select("SELECT * FROM note WHERE id = #{id} LIMIT 1")
    Note selectAdminById(@Param("id") Long id);

    @Select("""
        SELECT * FROM note
        WHERE status = #{status}
          AND (title LIKE CONCAT('%', #{keyword}, '%') OR content LIKE CONCAT('%', #{keyword}, '%'))
        ORDER BY create_time DESC LIMIT #{limit}
        """)
    List<Note> selectByKeyword(@Param("keyword") String keyword, @Param("status") int status, @Param("limit") int limit);
}
