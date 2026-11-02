package cc.adabyte.blog.common.gateway;

/**
 * 防腐层接口 — 站点维护的 Agent 写作指南（Markdown）。
 * agent-mcp 通过此接口读取，不直接依赖 system-config 的 Service/Mapper。
 */
public interface WritingGuideGateway {

    String getWritingGuide();
}
