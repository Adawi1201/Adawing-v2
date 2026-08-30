package cc.adabyte.blog.zoom.message.dto;

import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import cc.adabyte.blog.zoom.shared.enums.MessageRefType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访客端留言视图对象。
 *
 * <p>仅承载访客可见字段；{@code email} 等联系方式、{@code rejectReason}
 * 等管理信息不出现在访客端接口中。
 */
@Data
public class MessageVo {

    private Long id;
    private String nickname;
    private Long avatarResourceId;
    private String content;
    private ContentStatus status;
    private String reply;
    private Integer likeCount;
    private MessageRefType refType;
    private Long refId;
    private String refTitle;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
