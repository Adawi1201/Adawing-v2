package cc.adabyte.blog.resource.core.service.impl;

import cc.adabyte.blog.common.constants.ResourcePool;
import cc.adabyte.blog.common.exception.BusinessException;
import cc.adabyte.blog.common.result.PageResult;
import cc.adabyte.blog.resource.core.entity.Resource;
import cc.adabyte.blog.resource.core.mapper.ResourceMapper;
import cc.adabyte.blog.resource.core.service.ResourceAllocationFacade;
import cc.adabyte.blog.resource.core.service.ResourcePoolService;
import cc.adabyte.blog.resource.core.service.ResourceService;
import cc.adabyte.blog.resource.core.util.MarkdownResourceRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ResourceAllocationFacadeImpl implements ResourceAllocationFacade {

    private static final Pattern RESOURCE_PATTERN = Pattern.compile("resource://(\\d+)");

    private final ResourceService resourceService;
    private final ResourcePoolService poolService;
    private final ResourceMapper resourceMapper;
    private final MarkdownResourceRenderer renderer;

    @Override
    public Resource upload(MultipartFile file, ResourcePool pool) {
        return resourceService.upload(file, pool);
    }

    @Override
    public PageResult<Resource> pageByPool(ResourcePool pool, int page, int size) {
        List<Resource> all = poolService.listActiveByPool(pool);
        return paginate(all, page, size);
    }

    @Override
    public PageResult<Resource> pageAll(int page, int size) {
        List<Resource> all = resourceMapper.selectList(null);
        return paginate(all, page, size);
    }

    private PageResult<Resource> paginate(List<Resource> all, int page, int size) {
        int total = all.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        List<Resource> list = from < total ? all.subList(from, to) : new ArrayList<>();
        return PageResult.of((long) total, list, (long) page, (long) size);
    }

    @Override
    public Resource allocate(Long resourceId, ResourcePool targetPool) {
        return poolService.allocate(resourceId, targetPool);
    }

    @Override
    public void delete(Long resourceId) {
        Resource resource = resourceMapper.selectById(resourceId);
        if (resource == null) return;
        if (resource.getRefCount() != null && resource.getRefCount() > 0) {
            throw new BusinessException("资源已被引用，无法删除，请先解除引用");
        }
        resourceService.physicalDelete(resourceId);
    }

    @Override
    public List<Resource> listForUse(ResourcePool primary, boolean allowFallback) {
        return poolService.listForUse(primary, allowFallback);
    }

    @Override
    @Transactional
    public void bindArticleResources(Long articleId, Long coverResourceId, String content) {
        unbindArticleResources(articleId);

        List<Long> resourceIds = extractResourceIds(content);
        if (coverResourceId != null) {
            resourceIds.add(coverResourceId);
        }

        for (Long resourceId : resourceIds) {
            resourceService.bind(resourceId, "article", articleId);
        }
    }

    @Override
    @Transactional
    public void unbindArticleResources(Long articleId) {
        resourceService.unbindByObject("article", articleId);
    }

    @Override
    @Transactional
    public void bindMessageAvatar(Long messageId, Long avatarResourceId) {
        unbindMessageAvatar(messageId);
        if (avatarResourceId != null) {
            resourceService.bind(avatarResourceId, "message", messageId);
        }
    }

    @Override
    @Transactional
    public void unbindMessageAvatar(Long messageId) {
        resourceService.unbindByObject("message", messageId);
    }

    @Override
    public String renderMarkdown(String markdown) {
        return renderer.render(markdown);
    }

    private List<Long> extractResourceIds(String content) {
        List<Long> ids = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return ids;
        }
        Matcher matcher = RESOURCE_PATTERN.matcher(content);
        while (matcher.find()) {
            ids.add(Long.valueOf(matcher.group(1)));
        }
        return ids;
    }
}
