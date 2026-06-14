package cc.adabyte.blog.boot.service;

import cc.adabyte.blog.common.gateway.ArticleSearchGateway;
import cc.adabyte.blog.zoom.article.mapper.ArticleMapper;
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
        return articleMapper.selectByKeyword(keyword, cappedLimit).stream()
                .map(a -> new Result(a.getId(), a.getTitle(), a.getSummary()))
                .toList();
    }
}
