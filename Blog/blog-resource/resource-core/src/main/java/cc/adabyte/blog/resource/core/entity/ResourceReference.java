package cc.adabyte.blog.resource.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resource_reference")
public class ResourceReference {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resourceId;
    private String module;
    private Long objectId;
    private LocalDateTime createdAt;
}
