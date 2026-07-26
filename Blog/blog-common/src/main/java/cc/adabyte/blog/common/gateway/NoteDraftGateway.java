package cc.adabyte.blog.common.gateway;

/**
 * 防腐层接口 — note 草稿创建。
 * agent-mcp 通过此接口创建 note 草稿并进入审核流，不直接依赖 zoom-note 的 Service/Mapper。
 */
public interface NoteDraftGateway {
    /**
     * @param type note 类型：PERSONAL 或 TECH
     * @return 草稿 note ID
     */
    Long createNoteDraft(String title, String content, String type, String sourceAgent);
}
