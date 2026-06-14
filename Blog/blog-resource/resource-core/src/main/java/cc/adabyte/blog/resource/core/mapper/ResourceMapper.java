package cc.adabyte.blog.resource.core.mapper;

import cc.adabyte.blog.resource.core.entity.Resource;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface ResourceMapper extends BaseMapper<Resource> {
    @Select("SELECT * FROM resource WHERE status = 'ACTIVE' AND orphaned_at IS NOT NULL AND orphaned_at < #{before}")
    List<Resource> selectOrphans(@Param("before") LocalDateTime before);

    @Select("SELECT * FROM resource WHERE pool = #{pool} ORDER BY created_at DESC")
    List<Resource> selectByPool(@Param("pool") String pool);

    @Select("SELECT * FROM resource WHERE pool = #{pool} AND status = 'ACTIVE' ORDER BY created_at DESC")
    List<Resource> selectActiveByPool(@Param("pool") String pool);

    @Update("UPDATE resource SET ref_count = ref_count + 1 WHERE id = #{id}")
    int incrementRefCount(@Param("id") Long id);

    @Update("UPDATE resource SET ref_count = ref_count - 1 WHERE id = #{id}")
    int decrementRefCount(@Param("id") Long id);
}
