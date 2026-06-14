package cc.adabyte.blog.system.review.entity;

import cc.adabyte.blog.common.constants.ReviewStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_task")
public class ReviewTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String contentType;
    private Long contentId;
    private ReviewStatus status;
    private String submitterType;
    private String submitterId;
    private String rejectReason;
    private String reviewerNote;
    private LocalDateTime submitTime;
    private LocalDateTime reviewTime;
}
