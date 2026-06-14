package cc.adabyte.blog.zoom.tag.mapper;

import cc.adabyte.blog.zoom.tag.entity.Tag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TagMapper extends BaseMapper<Tag> {
    @Select("SELECT * FROM tag WHERE name LIKE CONCAT('%', #{name}, '%') LIMIT 10")
    List<Tag> selectSimilar(@Param("name") String name);
}
