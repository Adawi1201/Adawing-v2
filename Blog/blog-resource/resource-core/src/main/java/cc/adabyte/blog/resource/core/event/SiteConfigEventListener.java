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
        if (event.resourceIds() == null || event.resourceIds().isEmpty()) {
            log.info("[Resource] 站点配置已保存，无资源绑定，清除旧绑定");
            resourceService.unbindByObject(MODULE, OBJECT_ID);
            return;
        }
        log.info("[Resource] 站点配置已保存，更新资源绑定: {} 个资源 ids={}", event.resourceIds().size(), event.resourceIds());
        resourceService.unbindByObject(MODULE, OBJECT_ID);
        for (Long resourceId : event.resourceIds()) {
            log.info("[Resource] 绑定资源到站点配置: resourceId={}", resourceId);
            resourceService.bind(resourceId, MODULE, OBJECT_ID);
        }
        log.info("[Resource] 站点配置资源绑定完成");
    }
}
