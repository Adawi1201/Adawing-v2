package cc.adabyte.blog.common.gateway;

import java.util.List;

/**
 * 防腐层接口 — note 详情查询与搜索。
 * agent-mcp 通过此接口获取 note（含 Markdown 原文），不直接依赖 zoom-note 的 Service/Mapper。
 */
public interface NoteQueryGateway {

    Result getById(Long id);

    List<Result> search(String keyword, int limit);

    record Result(Long id, String title, String content, String type, String sourceAgent) {}
}
