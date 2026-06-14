package cc.adabyte.blog.zoom.article.entity;

import cc.adabyte.blog.zoom.shared.enums.ArticleSource;
import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article")
public class Article {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String summary;
    private String content;
    private Long coverResourceId;
    private ContentStatus status;
    private ArticleSource source;
    private String sourceAgent;
    @TableField("is_top")
    private Boolean top;
    @TableField("is_hidden")
    private Boolean hidden;
    private Integer viewCount;
    private String rejectReason;
    private String reviewerNote;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
