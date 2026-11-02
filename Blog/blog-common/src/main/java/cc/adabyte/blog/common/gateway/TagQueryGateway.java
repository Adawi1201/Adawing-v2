package cc.adabyte.blog.common.gateway;

import java.util.List;

/**
 * 防腐层接口 — 标签词表查询。
 * agent-mcp 通过此接口获取已有标签名，不直接依赖 zoom-tag 的 Service/Mapper。
 */
public interface TagQueryGateway {

    List<String> listNames();
}
