package cc.adabyte.blog.resource.core.service;

import cc.adabyte.blog.common.constants.ResourcePool;
import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.resource.core.entity.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResourceAllocationFacade {
    Resource upload(MultipartFile file, ResourcePool pool);
    PageResult<Resource> pageByPool(ResourcePool pool, int page, int size);
    PageResult<Resource> pageAll(int page, int size);
    Resource allocate(Long resourceId, ResourcePool targetPool);
    void delete(Long resourceId);
    List<Resource> listForUse(ResourcePool primary, boolean allowFallback);
    void bindArticleResources(Long articleId, Long coverResourceId, String content);
    void unbindArticleResources(Long articleId);
    void bindMessageAvatar(Long messageId, Long avatarResourceId);
    void unbindMessageAvatar(Long messageId);
    String renderMarkdown(String markdown);
}
