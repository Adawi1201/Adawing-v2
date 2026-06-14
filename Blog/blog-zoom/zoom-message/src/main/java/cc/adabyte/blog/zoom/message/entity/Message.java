package cc.adabyte.blog.zoom.message.entity;

import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import cc.adabyte.blog.zoom.shared.enums.MessageRefType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String nickname;
    private String email;
    @TableField("avatar_resource_id")
    private Long avatarResourceId;
    private String content;
    private ContentStatus status;
    private String reply;
    private String rejectReason;
    private Integer likeCount;
    private MessageRefType refType;
    private Long refId;
    private String refTitle;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
