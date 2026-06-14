package cc.adabyte.blog.common.model;

import lombok.Data;

@Data
public class SubmitReviewRequest {
    private String contentType;
    private Long contentId;
    private String submitterType;
    private String submitterId;
}
