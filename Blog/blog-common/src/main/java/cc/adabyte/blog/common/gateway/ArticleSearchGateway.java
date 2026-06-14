package cc.adabyte.blog.common.gateway;

import java.util.List;

/**
 * 防腐层接口 — 文章搜索。
 * agent-mcp 通过此接口搜索已有文章，不直接依赖 zoom-article 的 Service/Mapper。
 */
public interface ArticleSearchGateway {

    List<Result> search(String keyword, int limit);

    record Result(Long id, String title, String summary) {}
}
