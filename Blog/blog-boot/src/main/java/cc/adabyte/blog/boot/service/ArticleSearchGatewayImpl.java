package cc.adabyte.blog.boot.service;

import cc.adabyte.blog.common.gateway.ArticleSearchGateway;
import cc.adabyte.blog.zoom.article.mapper.ArticleMapper;
import cc.adabyte.blog.zoom.shared.enums.ContentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ArticleSearchGatewayImpl implements ArticleSearchGateway {

    private final ArticleMapper articleMapper;

    @Override
    public List<Result> search(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) return List.of();
        int cappedLimit = Math.min(limit, 20);
        return articleMapper.selectPublishedByKeyword(keyword, ContentStatus.PUBLISHED.getValue(), cappedLimit).stream()
                .map(a -> new Result(a.getId(), a.getTitle(), a.getSummary()))
                .toList();
    }
}
