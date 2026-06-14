package cc.adabyte.blog.zoom.message.mapper;

import cc.adabyte.blog.zoom.message.entity.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface MessageMapper extends BaseMapper<Message> {

    @Select("SELECT * FROM message WHERE status = #{status} ORDER BY create_time DESC")
    IPage<Message> selectByStatus(Page<Message> page, @Param("status") int status);

    @Select("SELECT * FROM message WHERE #{status} IS NULL OR status = #{status} ORDER BY create_time DESC")
    IPage<Message> selectAdmin(Page<Message> page, @Param("status") Integer status);

    @Select("SELECT * FROM message ORDER BY create_time DESC")
    IPage<Message> selectPage(Page<Message> page);
}
