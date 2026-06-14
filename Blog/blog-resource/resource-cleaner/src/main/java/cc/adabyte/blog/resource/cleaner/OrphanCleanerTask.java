package cc.adabyte.blog.resource.cleaner;

import cc.adabyte.blog.common.constants.ResourcePool;
import cc.adabyte.blog.resource.core.entity.Resource;
import cc.adabyte.blog.resource.core.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanCleanerTask {
    private final ResourceService resourceService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void clean() {
        log.info("[OrphanCleaner] 开始清理孤儿资源");
        List<Resource> orphans = resourceService.listOrphans(7);
        int count = 0;
        for (Resource r : orphans) {
            if (r.getPool() != ResourcePool.MISC) {
                continue;
            }
            try {
                resourceService.physicalDelete(r.getId());
                log.info("[OrphanCleaner] 已删除资源: id={}, url={}", r.getId(), r.getUrl());
                count++;
            } catch (Exception e) {
                log.error("[OrphanCleaner] 删除资源失败: id={}", r.getId(), e);
            }
        }
        log.info("[OrphanCleaner] 本次清理完成, 共处理 {} 条", count);
    }
}
