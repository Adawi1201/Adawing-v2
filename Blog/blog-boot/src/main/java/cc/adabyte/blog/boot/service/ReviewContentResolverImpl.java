package cc.adabyte.blog.boot.service;

import cc.adabyte.blog.common.gateway.ReviewContentResolver;
import cc.adabyte.blog.zoom.article.entity.Article;
import cc.adabyte.blog.zoom.article.mapper.ArticleMapper;
import cc.adabyte.blog.zoom.message.entity.Message;
import cc.adabyte.blog.zoom.message.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewContentResolverImpl implements ReviewContentResolver {

    private final MessageMapper messageMapper;
    private final ArticleMapper articleMapper;

    @Override
    public Result resolve(String contentType, Long contentId) {
        if ("message".equals(contentType)) {
            return resolveMessage(contentId);
        }
        if ("article".equals(contentType)) {
            return resolveArticle(contentId);
        }
        return null;
    }

    private Result resolveMessage(Long id) {
        Message msg = messageMapper.selectById(id);
        if (msg == null) return null;
        return new Result(
                msg.getNickname(),
                msg.getContent(),
                msg.getNickname(),
                msg.getEmail(),
                msg.getAvatarResourceId(),
                msg.getStatus() != null ? msg.getStatus().getValue() : null
        );
    }

    private Result resolveArticle(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) return null;
        return new Result(
                article.getTitle(),
                article.getContent(),
                article.getSourceAgent() != null ? article.getSourceAgent() : "Admin",
                null,
                article.getCoverResourceId(),
                article.getStatus() != null ? article.getStatus().getValue() : null
        );
    }
}
