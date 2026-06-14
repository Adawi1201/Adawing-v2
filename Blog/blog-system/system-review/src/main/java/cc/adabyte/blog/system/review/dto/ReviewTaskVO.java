package cc.adabyte.blog.system.review.dto;

import cc.adabyte.blog.system.review.entity.ReviewTask;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReviewTaskVO extends ReviewTask {
    private String contentTitle;
    private String contentBody;
    private String submitterName;
    private String submitterEmail;
    private Long avatarResourceId;
    private Integer contentStatus;
}
