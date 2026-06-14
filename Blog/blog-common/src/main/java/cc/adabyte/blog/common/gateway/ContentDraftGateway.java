package cc.adabyte.blog.common.gateway;

public interface ContentDraftGateway {
    Long createArticleDraft(String title, String content, String summary, String sourceAgent);
}
