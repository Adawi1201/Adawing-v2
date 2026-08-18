package cc.adabyte.blog.system.config.dto;

import lombok.Data;

@Data
public class ExternalLinkDto {
    private String section;
    private String name;
    private String url;
    private String description;
    private String icon;
}
