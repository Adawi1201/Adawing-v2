package cc.adabyte.blog.resource.core.service;

import cc.adabyte.blog.common.constants.ResourcePool;
import cc.adabyte.blog.resource.core.entity.Resource;

import java.util.List;

public interface ResourcePoolService {
    void validate(ResourcePool pool);
    List<Resource> listActiveByPool(ResourcePool pool);
    List<Resource> listForUse(ResourcePool primary, boolean allowFallback);
    Resource allocate(Long resourceId, ResourcePool targetPool);
}
