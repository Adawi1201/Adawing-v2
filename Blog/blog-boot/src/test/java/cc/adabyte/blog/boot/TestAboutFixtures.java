package cc.adabyte.blog.boot;

import cc.adabyte.blog.system.config.dto.AboutDto;
import cc.adabyte.blog.system.config.dto.AboutLinksDto;
import cc.adabyte.blog.system.config.dto.AboutSiteInfoDto;
import cc.adabyte.blog.system.config.dto.AbilityDto;
import cc.adabyte.blog.system.config.dto.ContactDto;
import cc.adabyte.blog.system.config.dto.ExperienceItemDto;
import cc.adabyte.blog.system.config.dto.ExternalLinkDto;
import cc.adabyte.blog.system.config.dto.LicenseDto;
import cc.adabyte.blog.system.config.dto.PinDto;
import cc.adabyte.blog.system.config.dto.SiteContentDto;
import cc.adabyte.blog.system.config.dto.DevStackItemDto;
import cc.adabyte.blog.system.config.dto.SocialLinkDto;

import java.util.List;

final class TestAboutFixtures {

    private TestAboutFixtures() {
    }

    static AboutDto withResourceIds() {
        ExperienceItemDto experience = new ExperienceItemDto();
        experience.setIcon("3");
        PinDto pin = new PinDto();
        pin.setAvatar("4");
        pin.setExperience(List.of(experience));

        DevStackItemDto stack = new DevStackItemDto();
        stack.setIcon("5");
        AbilityDto ability = new AbilityDto();
        ability.setDevStack(List.of(stack));

        SocialLinkDto social = new SocialLinkDto();
        social.setIcon("7");
        ContactDto contact = new ContactDto();
        contact.setOtherSocialPlatform(List.of(social));

        ExternalLinkDto github = new ExternalLinkDto();
        github.setIcon("8");
        ExternalLinkDto bilibili = new ExternalLinkDto();
        bilibili.setIcon("9");
        AboutLinksDto links = new AboutLinksDto();
        links.setItems(List.of(github, bilibili));

        AboutSiteInfoDto siteInfo = new AboutSiteInfoDto();
        LicenseDto license = new LicenseDto();
        siteInfo.setLicense(license);

        SiteContentDto content = new SiteContentDto();

        AboutDto about = new AboutDto();
        about.setPin(pin);
        about.setAbility(ability);
        about.setContact(contact);
        about.setLinks(links);
        about.setSiteInfo(siteInfo);
        about.setSiteContent(content);
        return about;
    }
}
