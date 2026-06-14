package cc.adabyte.blog.system.auth.mapper;

import cc.adabyte.blog.system.auth.entity.McpKey;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface McpKeyMapper extends BaseMapper<McpKey> {
    @Select("SELECT * FROM mcp_key WHERE key_hash = #{hash} AND enabled = 1 LIMIT 1")
    McpKey selectByKeyHash(@Param("hash") String hash);
}
