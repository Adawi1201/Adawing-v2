package cc.adabyte.blog.common.gateway;

/**
 * 防腐层接口 — 审核内容删除。
 * system-review 通过此接口删除底层内容，不直接依赖业务模块。
 */
public interface ReviewContentDeletionGateway {

    void delete(String contentType, Long contentId);
}
