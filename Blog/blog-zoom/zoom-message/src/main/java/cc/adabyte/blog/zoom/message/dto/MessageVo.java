package cc.adabyte.blog.zoom.message.dto;

import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import cc.adabyte.blog.zoom.shared.enums.MessageRefType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访客端留言视图对象。
 *
 * <p>故意排除 {@code email} 与 {@code rejectReason} 等敏感/管理字段，
 * 防止访客列表接口泄露留言者隐私信息。
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
