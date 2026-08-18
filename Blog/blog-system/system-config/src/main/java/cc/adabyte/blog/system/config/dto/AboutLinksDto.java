package cc.adabyte.blog.system.config.dto;

import lombok.Data;

import java.util.List;

@Data
public class AboutLinksDto {
    private List<ExternalLinkDto> items;
}
