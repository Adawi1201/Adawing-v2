package cc.adabyte.blog.boot;

import cc.adabyte.blog.common.exception.BusinessException;
import cc.adabyte.blog.zoom.message.entity.Message;
import cc.adabyte.blog.zoom.message.mapper.MessageMapper;
import cc.adabyte.blog.zoom.message.service.MessageLikeBuffer;
import cc.adabyte.blog.zoom.message.service.MessageService;
import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 留言点赞缓冲刷库回归测试。
 * 覆盖 Spec AC3/AC5/AC6：已发布留言点赞内存累加、返回值含增量、sync 后落库、非 PUBLISHED 拒绝。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DisplayName("留言点赞缓冲刷库测试")
class MessageLikeTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private MessageLikeBuffer likeBuffer;

    @BeforeEach
    void clearBuffer() {
        likeBuffer.drain();
    }

    private Message insertMessage(ContentStatus status) {
        Message m = new Message();
        m.setNickname("测试者");
        m.setEmail("t@example.com");
        m.setContent("点赞测试留言");
        m.setStatus(status);
        m.setLikeCount(0);
        m.setCreateTime(LocalDateTime.now());
        m.setUpdateTime(LocalDateTime.now());
        messageMapper.insert(m);
        return m;
    }

    @Test
    @DisplayName("AC3/AC6 — 点赞内存累加，返回值含增量，sync 前 DB 不变")
    void accumulatesInMemory() {
        Message seed = insertMessage(ContentStatus.PUBLISHED);

        assertEquals(1, messageService.like(seed.getId()), "首次点赞返回 1");
        assertEquals(2, messageService.like(seed.getId()), "再次点赞返回 2");

        assertEquals(0, messageMapper.selectById(seed.getId()).getLikeCount(), "sync 前 DB 应仍为 0");
        assertEquals(2, likeBuffer.peek(seed.getId()), "内存增量应为 2");
    }

    @Test
    @DisplayName("AC6 — sync 后增量落库，缓冲清零")
    void syncFlushesToDb() {
        Message seed = insertMessage(ContentStatus.PUBLISHED);

        messageService.like(seed.getId());
        messageService.like(seed.getId());
        messageService.like(seed.getId());

        messageService.syncLikeCountsToDb();

        assertEquals(3, messageMapper.selectById(seed.getId()).getLikeCount(), "sync 后 DB 应为 3");
        assertEquals(0, likeBuffer.peek(seed.getId()), "sync 后内存增量应清零");
    }

    @Test
    @DisplayName("AC5 — 非 PUBLISHED 留言点赞被拒绝且不累加")
    void nonPublishedRejected() {
        Message pending = insertMessage(ContentStatus.PENDING_REVIEW);

        assertThrows(BusinessException.class, () -> messageService.like(pending.getId()), "待审核留言不可点赞");
        assertEquals(0, likeBuffer.peek(pending.getId()), "被拒绝的点赞不应累加");
    }

    @Test
    @DisplayName("AC5 — 点赞不存在留言抛出业务异常")
    void nonExistentRejected() {
        assertThrows(BusinessException.class, () -> messageService.like(999_999_999L));
    }
}
