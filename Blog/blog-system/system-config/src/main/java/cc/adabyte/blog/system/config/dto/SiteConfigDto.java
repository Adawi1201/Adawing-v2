package cc.adabyte.blog.system.config.dto;

import lombok.Data;

import java.util.List;

@Data
public class SiteConfigDto {
    private String name;
    private String description;
    private String subtitle;
    private String logo;
    private String favicon;
    private String icp;
    private String publicSecurityRecord;
    private String footerText;
    private SeoDto seo;
    private ProfileDto profile;
    private List<LinkDto> links;
}
