package cc.adabyte.blog.common.model;

import java.time.LocalDateTime;

public interface Content {
    Long getId();
    String getTitle();
    String getContent();
    LocalDateTime getCreateTime();
}
