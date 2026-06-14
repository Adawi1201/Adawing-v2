package cc.adabyte.blog.zoom.note.mapper;

import cc.adabyte.blog.zoom.note.entity.Note;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface NoteMapper extends BaseMapper<Note> {
    @Select("SELECT * FROM note WHERE type = #{type} ORDER BY create_time DESC")
    IPage<Note> selectByType(Page<Note> page, @Param("type") int type);

    @Select("SELECT * FROM note ORDER BY create_time DESC")
    IPage<Note> selectAll(Page<Note> page);
}
