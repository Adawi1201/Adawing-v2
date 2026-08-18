package cc.adabyte.blog.system.config.dto;

import lombok.Data;

@Data
public class LicenseDto {
    private boolean enabled;
    private String name;
    private String url;
}
