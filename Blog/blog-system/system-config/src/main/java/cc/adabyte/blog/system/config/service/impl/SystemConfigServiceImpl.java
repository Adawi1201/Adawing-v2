package cc.adabyte.blog.system.config.service.impl;

import cc.adabyte.blog.system.config.dto.LinkDto;
import cc.adabyte.blog.system.config.dto.ProfileDto;
import cc.adabyte.blog.system.config.dto.SeoDto;
import cc.adabyte.blog.system.config.dto.SiteConfigDto;
import cc.adabyte.blog.system.config.entity.SystemConfig;
import cc.adabyte.blog.system.config.mapper.SystemConfigMapper;
import cc.adabyte.blog.system.config.service.SystemConfigService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final String SITE_CONFIG_KEY = "site.config";

    private final SystemConfigMapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    public SiteConfigDto getSiteConfig() {
        SystemConfig config = mapper.selectByKey(SITE_CONFIG_KEY);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return defaultConfig();
        }
        try {
            return objectMapper.readValue(config.getConfigValue(), SiteConfigDto.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse site config json: {}", config.getConfigValue(), e);
            return defaultConfig();
        }
    }

    @Override
    @Transactional
    public void saveSiteConfig(SiteConfigDto dto) {
        SystemConfig existing = mapper.selectByKey(SITE_CONFIG_KEY);
        try {
            String json = objectMapper.writeValueAsString(dto);
            if (existing != null) {
                existing.setConfigValue(json);
                existing.setUpdateTime(LocalDateTime.now());
                mapper.updateById(existing);
            } else {
                SystemConfig config = new SystemConfig();
                config.setConfigKey(SITE_CONFIG_KEY);
                config.setConfigValue(json);
                config.setDescription("站点统一配置");
                config.setCreateTime(LocalDateTime.now());
                config.setUpdateTime(LocalDateTime.now());
                mapper.insert(config);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize site config", e);
        }
    }

    private SiteConfigDto defaultConfig() {
        SiteConfigDto dto = new SiteConfigDto();
        dto.setName("AdaWing");
        dto.setDescription("");
        dto.setSubtitle("");
        dto.setLogo("");
        dto.setFavicon("");
        dto.setIcp("");
        dto.setPublicSecurityRecord("");
        dto.setFooterText("");

        SeoDto seo = new SeoDto();
        seo.setKeywords("");
        seo.setDescription("");
        dto.setSeo(seo);

        ProfileDto profile = new ProfileDto();
        profile.setOwnerName("");
        profile.setAvatar("");
        profile.setBio("");
        dto.setProfile(profile);

        LinkDto github = new LinkDto();
        github.setName("GitHub");
        github.setUrl("https://example.com");
        github.setType("social");
        github.setIcon("");

        LinkDto email = new LinkDto();
        email.setName("Email");
        email.setUrl("mailto:hi@example.com");
        email.setType("social");
        email.setIcon("");

        LinkDto friend = new LinkDto();
        friend.setName("Friend Link");
        friend.setUrl("https://example.com");
        friend.setType("friend");
        friend.setIcon("");

        dto.setLinks(List.of(github, email, friend));
        return dto;
    }
}
