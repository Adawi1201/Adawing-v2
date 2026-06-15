package cc.adabyte.blog.resource.core.event;

import cc.adabyte.blog.common.event.SiteConfigSavedEvent;
import cc.adabyte.blog.resource.core.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SiteConfigEventListener {

    private static final String MODULE = "site_config";
    private static final Long OBJECT_ID = 0L;

    private final ResourceService resourceService;

    @EventListener
    public void onSiteConfigSaved(SiteConfigSavedEvent event) {
        log.info("[Resource] 站点配置已保存，更新资源绑定: ids={}", event.resourceIds());
        resourceService.unbindByObject(MODULE, OBJECT_ID);
        for (Long resourceId : event.resourceIds()) {
            resourceService.bind(resourceId, MODULE, OBJECT_ID);
        }
    }
}
