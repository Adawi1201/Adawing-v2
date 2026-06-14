package cc.adabyte.blog.system.config.mapper;

import cc.adabyte.blog.system.config.entity.SystemConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SystemConfigMapper extends BaseMapper<SystemConfig> {
    @Select("SELECT * FROM system_config WHERE config_key = #{key} LIMIT 1")
    SystemConfig selectByKey(@Param("key") String key);
}
