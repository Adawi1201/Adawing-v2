package cc.adabyte.blog.system.config.service.impl;

import cc.adabyte.blog.common.event.SiteConfigSavedEvent;
import cc.adabyte.blog.system.config.dto.AboutDto;
import cc.adabyte.blog.system.config.dto.AboutLinksDto;
import cc.adabyte.blog.system.config.dto.AboutSiteInfoDto;
import cc.adabyte.blog.system.config.dto.AbilityDto;
import cc.adabyte.blog.system.config.dto.ContactDto;
import cc.adabyte.blog.system.config.dto.ExperienceItemDto;
import cc.adabyte.blog.system.config.dto.ExternalLinkDto;
import cc.adabyte.blog.system.config.dto.LicenseDto;
import cc.adabyte.blog.system.config.dto.PinDto;
import cc.adabyte.blog.system.config.dto.SeoDto;
import cc.adabyte.blog.system.config.dto.DevStackItemDto;
import cc.adabyte.blog.system.config.dto.SocialLinkDto;
import cc.adabyte.blog.system.config.dto.SiteContentDto;
import cc.adabyte.blog.system.config.dto.SiteConfigDto;
import cc.adabyte.blog.system.config.entity.SystemConfig;
import cc.adabyte.blog.system.config.mapper.SystemConfigMapper;
import cc.adabyte.blog.system.config.service.SystemConfigService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final String SITE_CONFIG_KEY = "site.config";

    private final SystemConfigMapper mapper;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public SiteConfigDto getSiteConfig() {
        SystemConfig config = mapper.selectByKey(SITE_CONFIG_KEY);
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return defaultConfig();
        }
        try {
            return normalize(objectMapper.readValue(config.getConfigValue(), SiteConfigDto.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to parse site config json: {}", config.getConfigValue(), e);
            return defaultConfig();
        }
    }

    @Override
    @Transactional
    public void saveSiteConfig(SiteConfigDto dto) {
        dto = normalize(dto);
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

        Set<Long> resourceIds = collectResourceIds(dto);
        eventPublisher.publishEvent(new SiteConfigSavedEvent(resourceIds));
    }

    private Set<Long> collectResourceIds(SiteConfigDto dto) {
        Set<Long> ids = new HashSet<>();
        addIfPresent(ids, dto.getLogo());
        addIfPresent(ids, dto.getFavicon());
        AboutDto about = dto.getAbout();
        if (about != null) {
            PinDto pin = about.getPin();
            if (pin != null) {
                addIfPresent(ids, pin.getAvatar());
                if (pin.getExperience() != null) {
                    for (ExperienceItemDto item : pin.getExperience()) {
                        if (item != null) addIfPresent(ids, item.getIcon());
                    }
                }
            }
            AbilityDto ability = about.getAbility();
            if (ability != null) {
                collectDevStackResources(ids, ability.getDevStack());
            }
            ContactDto contact = about.getContact();
            if (contact != null && contact.getOtherSocialPlatform() != null) {
                for (SocialLinkDto link : contact.getOtherSocialPlatform()) {
                    if (link != null) addIfPresent(ids, link.getIcon());
                }
            }
            AboutLinksDto links = about.getLinks();
            if (links != null) {
                collectExternalLinkResources(ids, links.getItems());
            }
        }
        return ids;
    }

    private void collectDevStackResources(Set<Long> ids, List<DevStackItemDto> skills) {
        if (skills == null) return;
        for (DevStackItemDto item : skills) {
            if (item != null) addIfPresent(ids, item.getIcon());
        }
    }

    private void collectExternalLinkResources(Set<Long> ids, List<ExternalLinkDto> links) {
        if (links == null) return;
        for (ExternalLinkDto link : links) {
            if (link != null) addIfPresent(ids, link.getIcon());
        }
    }

    private void addIfPresent(Set<Long> ids, String resourceId) {
        if (resourceId != null && !resourceId.isBlank()) {
            try {
                ids.add(Long.valueOf(resourceId));
            } catch (NumberFormatException e) {
                log.warn("Invalid resource id in site config: {}", resourceId);
            }
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

        dto.setAbout(emptyAbout());
        return dto;
    }

    private SiteConfigDto normalize(SiteConfigDto dto) {
        if (dto == null) return defaultConfig();

        AboutDto about = dto.getAbout();
        if (about == null) about = emptyAbout();
        if (about.getPin() == null) about.setPin(new PinDto());
        if (about.getPin().getExperience() == null) about.getPin().setExperience(new ArrayList<>());

        if (about.getAbility() == null) about.setAbility(new AbilityDto());
        if (about.getAbility().getDevStack() == null) about.getAbility().setDevStack(new ArrayList<>());
        if (about.getContact() == null) about.setContact(new ContactDto());
        if (about.getContact().getOtherSocialPlatform() == null) about.getContact().setOtherSocialPlatform(new ArrayList<>());
        if (about.getLinks() == null) about.setLinks(new AboutLinksDto());
        if (about.getLinks().getItems() == null) about.getLinks().setItems(new ArrayList<>());
        if (about.getSiteInfo() == null) about.setSiteInfo(new AboutSiteInfoDto());
        if (about.getSiteInfo().getLicense() == null) about.getSiteInfo().setLicense(new LicenseDto());
        if (about.getSiteContent() == null) about.setSiteContent(new SiteContentDto());

        dto.setAbout(about);
        return dto;
    }

    private AboutDto emptyAbout() {
        AboutDto about = new AboutDto();
        PinDto pin = new PinDto();
        pin.setExperience(new ArrayList<>());
        about.setPin(pin);
        AbilityDto ability = new AbilityDto();
        ability.setDevStack(new ArrayList<>());
        about.setAbility(ability);
        ContactDto contact = new ContactDto();
        contact.setOtherSocialPlatform(new ArrayList<>());
        about.setContact(contact);
        AboutLinksDto links = new AboutLinksDto();
        links.setItems(new ArrayList<>());
        about.setLinks(links);
        AboutSiteInfoDto siteInfo = new AboutSiteInfoDto();
        siteInfo.setLicense(new LicenseDto());
        about.setSiteInfo(siteInfo);
        SiteContentDto content = new SiteContentDto();
        about.setSiteContent(content);
        return about;
    }

}
