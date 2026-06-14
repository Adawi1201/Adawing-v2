package cc.adabyte.blog.resource.core.service.impl;

import cc.adabyte.blog.common.constants.ResourcePool;
import cc.adabyte.blog.common.exception.BusinessException;
import cc.adabyte.blog.resource.core.entity.Resource;
import cc.adabyte.blog.resource.core.mapper.ResourceMapper;
import cc.adabyte.blog.resource.core.service.ResourcePoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourcePoolServiceImpl implements ResourcePoolService {

    private final ResourceMapper resourceMapper;

    @Override
    public void validate(ResourcePool pool) {
        if (pool == null) {
            throw new BusinessException("资源池不能为空");
        }
    }

    @Override
    public List<Resource> listActiveByPool(ResourcePool pool) {
        validate(pool);
        return resourceMapper.selectActiveByPool(pool.name());
    }

    @Override
    public List<Resource> listForUse(ResourcePool primary, boolean allowFallback) {
        validate(primary);
        List<Resource> result = new ArrayList<>(listActiveByPool(primary));
        if (allowFallback && primary != ResourcePool.MISC) {
            result.addAll(listActiveByPool(ResourcePool.MISC));
        }
        return result;
    }

    @Override
    @Transactional
    public Resource allocate(Long resourceId, ResourcePool targetPool) {
        validate(targetPool);
        Resource resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new BusinessException("资源不存在");
        }
        resource.setPool(targetPool);
        resourceMapper.updateById(resource);
        return resource;
    }
}
