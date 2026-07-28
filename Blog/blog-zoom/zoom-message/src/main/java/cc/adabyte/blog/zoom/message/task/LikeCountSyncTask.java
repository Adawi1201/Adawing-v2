package cc.adabyte.blog.zoom.message.task;

import cc.adabyte.blog.zoom.message.service.MessageService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时将内存中的留言点赞增量批量刷回数据库。
 * 应用关闭时兜底刷回一次，避免丢失当周期增量。仿 {@code ViewCountSyncTask}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikeCountSyncTask {

    private final MessageService messageService;

    @Scheduled(cron = "${message.like-count.sync-cron:0 */5 * * * ?}")
    public void sync() {
        messageService.syncLikeCountsToDb();
    }

    @PreDestroy
    public void flushOnShutdown() {
        log.info("[MessageLike] 应用关闭，刷回残余点赞增量");
        messageService.syncLikeCountsToDb();
    }
}
