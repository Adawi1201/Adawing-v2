package cc.adabyte.blog.boot.service;

import cc.adabyte.blog.common.exception.BusinessException;
import cc.adabyte.blog.common.event.ArticleDeletedEvent;
import cc.adabyte.blog.common.gateway.ReviewContentDeletionGateway;
import cc.adabyte.blog.resource.core.service.ResourceAllocationFacade;
import cc.adabyte.blog.zoom.article.mapper.ArticleMapper;
import cc.adabyte.blog.zoom.article.mapper.ArticleTagMapper;
import cc.adabyte.blog.zoom.message.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewContentDeletionGatewayImpl implements ReviewContentDeletionGateway {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final MessageMapper messageMapper;
    private final ResourceAllocationFacade resourceFacade;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void delete(String contentType, Long contentId) {
        if ("article".equals(contentType)) {
            articleTagMapper.deleteByArticleId(contentId);
            articleMapper.deleteById(contentId);
            eventPublisher.publishEvent(new ArticleDeletedEvent(contentId));
            return;
        }
        if ("message".equals(contentType)) {
            resourceFacade.unbindMessageAvatar(contentId);
            messageMapper.deleteById(contentId);
            return;
        }
        throw new BusinessException("未知内容类型: " + contentType);
    }
}
