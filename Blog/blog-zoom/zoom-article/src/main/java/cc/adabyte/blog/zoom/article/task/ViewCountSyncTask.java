package cc.adabyte.blog.zoom.article.task;

import cc.adabyte.blog.zoom.article.service.ArticleService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时将内存中的浏览量增量批量刷回数据库。
 * 应用关闭时兜底刷回一次，避免丢失当周期增量。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountSyncTask {

    private final ArticleService articleService;

    @Scheduled(cron = "${article.view-count.sync-cron:0 */5 * * * ?}")
    public void sync() {
        articleService.syncViewCountsToDb();
    }

    @PreDestroy
    public void flushOnShutdown() {
        log.info("[ViewCount] 应用关闭，刷回残余浏览量增量");
        articleService.syncViewCountsToDb();
    }
}
