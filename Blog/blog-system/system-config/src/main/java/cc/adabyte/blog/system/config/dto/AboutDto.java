package cc.adabyte.blog.system.config.dto;

import lombok.Data;

@Data
public class AboutDto {
    private PinDto pin;
    private AbilityDto ability;
    private ContactDto contact;
    private AboutLinksDto links;
    private AboutSiteInfoDto siteInfo;
    private SiteContentDto siteContent;
}
