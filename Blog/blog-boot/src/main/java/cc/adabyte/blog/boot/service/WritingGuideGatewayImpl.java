package cc.adabyte.blog.boot.service;

import cc.adabyte.blog.common.gateway.WritingGuideGateway;
import cc.adabyte.blog.system.config.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WritingGuideGatewayImpl implements WritingGuideGateway {

    private final SystemConfigService systemConfigService;

    @Override
    public String getWritingGuide() {
        return systemConfigService.getWritingGuide();
    }
}
