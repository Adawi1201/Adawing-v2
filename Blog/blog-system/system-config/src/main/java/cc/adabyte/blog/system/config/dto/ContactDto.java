package cc.adabyte.blog.system.config.dto;

import lombok.Data;

import java.util.List;

@Data
public class ContactDto {
    private String email;
    private List<SocialLinkDto> otherSocialPlatform;
}
