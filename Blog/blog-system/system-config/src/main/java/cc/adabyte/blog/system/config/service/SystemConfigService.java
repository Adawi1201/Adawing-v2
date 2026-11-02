package cc.adabyte.blog.system.config.service;

import cc.adabyte.blog.system.config.dto.SiteConfigDto;

public interface SystemConfigService {
    SiteConfigDto getSiteConfig();
    void saveSiteConfig(SiteConfigDto dto);

    /** 供 Agent 起草内容时参考的写作指南（Markdown），未配置时返回空字符串。 */
    String getWritingGuide();
    void saveWritingGuide(String content);
}
