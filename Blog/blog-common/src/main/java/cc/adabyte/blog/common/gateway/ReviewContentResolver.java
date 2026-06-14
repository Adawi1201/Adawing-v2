package cc.adabyte.blog.common.gateway;

/**
 * 防腐层接口 — 审核内容详情查询。
 * system-review 通过此接口获取待审内容的详细信息，不直接依赖任何业务模块。
 */
public interface ReviewContentResolver {

    Result resolve(String contentType, Long contentId);

    record Result(String title, String content, String submitterName, String submitterEmail,
                  Long avatarResourceId, Integer currentStatus) {}
}
