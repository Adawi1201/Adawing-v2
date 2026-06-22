package cc.adabyte.blog.system.review.dto;

import lombok.Data;

@Data
public class ReviewActionRequest {
    private String reviewerNote;
    private String reason;
    private Long coverResourceId;
    private Long avatarResourceId;
}
