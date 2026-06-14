package cc.adabyte.blog.resource.core.entity;

import cc.adabyte.blog.common.constants.ResourcePool;
import cc.adabyte.blog.common.constants.ResourceStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resource")
public class Resource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String originalName;
    private String url;
    private Long size;
    private String mimeType;
    private ResourcePool pool;
    private Integer refCount;
    private ResourceStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime orphanedAt;
}
