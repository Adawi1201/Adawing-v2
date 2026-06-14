package cc.adabyte.blog.system.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mcp_key")
public class McpKey {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String keyHash;
    private Integer enabled;
    private String description;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createTime;
}
