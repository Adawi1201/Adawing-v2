package cc.adabyte.blog.system.config.dto;

import lombok.Data;

import java.util.List;

@Data
public class PinDto {
    private String ownerName;
    private String avatar;
    private String signature;
    private String job;
    private String unit;
    private List<ExperienceItemDto> experience;
}
