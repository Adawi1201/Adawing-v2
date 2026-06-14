package cc.adabyte.blog.system.config.service;

import cc.adabyte.blog.system.config.dto.SiteConfigDto;

public interface SystemConfigService {
    SiteConfigDto getSiteConfig();
    void saveSiteConfig(SiteConfigDto dto);
}
