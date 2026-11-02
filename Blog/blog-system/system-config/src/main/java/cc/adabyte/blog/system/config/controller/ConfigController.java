package cc.adabyte.blog.system.config.controller;

import cc.adabyte.blog.common.result.Result;
import cc.adabyte.blog.system.config.dto.SiteConfigDto;
import cc.adabyte.blog.system.config.dto.WritingGuideDto;
import cc.adabyte.blog.system.config.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/config")
@RequiredArgsConstructor
public class ConfigController {

    private final SystemConfigService configService;

    @GetMapping("/site")
    public Result<SiteConfigDto> getSiteConfig() {
        return Result.ok(configService.getSiteConfig());
    }

    @PutMapping("/site")
    public Result<Void> saveSiteConfig(@RequestBody SiteConfigDto dto) {
        configService.saveSiteConfig(dto);
        return Result.ok();
    }

    @GetMapping("/writing-guide")
    public Result<WritingGuideDto> getWritingGuide() {
        WritingGuideDto dto = new WritingGuideDto();
        dto.setContent(configService.getWritingGuide());
        return Result.ok(dto);
    }

    @PutMapping("/writing-guide")
    public Result<Void> saveWritingGuide(@RequestBody WritingGuideDto dto) {
        configService.saveWritingGuide(dto.getContent());
        return Result.ok();
    }
}
