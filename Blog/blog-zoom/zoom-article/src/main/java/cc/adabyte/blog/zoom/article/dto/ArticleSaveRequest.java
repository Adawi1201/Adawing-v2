package cc.adabyte.blog.zoom.article.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ArticleSaveRequest {
    private Long id;
    @NotBlank
    private String title;
    private String summary;
    private String content;
    private Long coverResourceId;
    private List<String> tagNames;
}
