package cc.adabyte.blog.boot.service;

import cc.adabyte.blog.common.gateway.ArticleQueryGateway;
import cc.adabyte.blog.zoom.article.entity.Article;
import cc.adabyte.blog.zoom.article.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleQueryGatewayImpl implements ArticleQueryGateway {

    private final ArticleMapper articleMapper;

    @Override
    public Result getById(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) return null;
        return new Result(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                article.getSummary(),
                article.getSourceAgent()
        );
    }
}
