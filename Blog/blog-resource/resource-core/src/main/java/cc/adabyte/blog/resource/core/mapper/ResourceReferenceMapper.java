package cc.adabyte.blog.resource.core.mapper;

import cc.adabyte.blog.resource.core.entity.ResourceReference;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ResourceReferenceMapper extends BaseMapper<ResourceReference> {
    @Select("SELECT * FROM resource_reference WHERE module = #{module} AND object_id = #{objectId}")
    List<ResourceReference> selectByObject(@Param("module") String module, @Param("objectId") Long objectId);

    @Delete("DELETE FROM resource_reference WHERE resource_id = #{resourceId}")
    void deleteByResourceId(@Param("resourceId") Long resourceId);

    @Delete("DELETE FROM resource_reference WHERE module = #{module} AND object_id = #{objectId}")
    void deleteByObject(@Param("module") String module, @Param("objectId") Long objectId);
}
