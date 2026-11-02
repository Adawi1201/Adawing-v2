package cc.adabyte.blog.common.gateway;

import java.util.List;

public interface ContentDraftGateway {
    Long createArticleDraft(String title, String content, String summary, String sourceAgent, List<String> tagNames);
}
