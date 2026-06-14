package cc.adabyte.blog.common.gateway;

/**
 * 防腐层接口 — 文章详情查询。
 * agent-mcp 通过此接口获取文章详情（含 Markdown 原文），不直接依赖 zoom-article 的 Service/Mapper。
 */
public interface ArticleQueryGateway {

    Result getById(Long id);

    record Result(Long id, String title, String content, String summary, String sourceAgent) {}
}
