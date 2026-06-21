package cc.adabyte.blog.system.config.controller;

import cc.adabyte.blog.common.result.Result;
import cc.adabyte.blog.system.config.dto.SiteConfigDto;
import cc.adabyte.blog.system.config.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/system/config")
@RequiredArgsConstructor
public class DashboardController {

    private final SystemConfigService configService;

    @GetMapping("/dashboard")
    public Result<SiteConfigDto> dashboard() {
        return Result.ok(configService.getSiteConfig());
    }
}
