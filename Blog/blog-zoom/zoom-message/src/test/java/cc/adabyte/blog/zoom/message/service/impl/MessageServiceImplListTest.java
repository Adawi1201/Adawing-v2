package cc.adabyte.blog.zoom.message.service.impl;

import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.resource.core.service.ResourceAllocationFacade;
import cc.adabyte.blog.system.review.service.ReviewService;
import cc.adabyte.blog.zoom.message.dto.MessageVo;
import cc.adabyte.blog.zoom.message.entity.Message;
import cc.adabyte.blog.zoom.message.mapper.MessageMapper;
import cc.adabyte.blog.zoom.message.service.MailService;
import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplListTest {

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private ResourceAllocationFacade resourceFacade;

    @Mock
    private MailService mailService;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private MessageServiceImpl messageService;

    @Test
    void visitorListShouldNotExposeEmail() {
        Message message = new Message();
        message.setId(1L);
        message.setNickname("visitor");
        message.setEmail("visitor@example.com");
        message.setContent("hello");
        message.setStatus(ContentStatus.PUBLISHED);

        Page<Message> page = new Page<>(1, 10);
        page.setRecords(List.of(message));
        page.setTotal(1);
        when(messageMapper.selectByStatus(any(Page.class), eq(ContentStatus.PUBLISHED.getValue())))
                .thenReturn(page);
        when(resourceFacade.renderMarkdown("hello")).thenReturn("hello");

        PageResult<MessageVo> result = messageService.list(1, 10);

        assertEquals(1, result.getList().size());
        MessageVo vo = result.getList().get(0);
        assertEquals("visitor", vo.getNickname());
        assertEquals("hello", vo.getContent());
        assertThrows(NoSuchFieldException.class,
                () -> MessageVo.class.getDeclaredField("email"),
                "MessageVo 不应包含 email 字段");
    }
}
