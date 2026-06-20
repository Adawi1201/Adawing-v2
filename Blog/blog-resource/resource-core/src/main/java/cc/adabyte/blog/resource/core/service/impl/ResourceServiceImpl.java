package cc.adabyte.blog.resource.core.service.impl;

import cc.adabyte.blog.common.constants.ResourcePool;
import cc.adabyte.blog.common.constants.ResourceStatus;
import cc.adabyte.blog.common.exception.BusinessException;
import cc.adabyte.blog.resource.core.entity.Resource;
import cc.adabyte.blog.resource.core.entity.ResourceReference;
import cc.adabyte.blog.resource.core.mapper.ResourceMapper;
import cc.adabyte.blog.resource.core.mapper.ResourceReferenceMapper;
import cc.adabyte.blog.resource.core.service.ResourceDownload;
import cc.adabyte.blog.resource.core.service.ResourceService;
import cc.adabyte.blog.resource.oss.OssTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import cc.adabyte.blog.resource.core.util.FileValidator;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceMapper resourceMapper;
    private final ResourceReferenceMapper referenceMapper;
    private final OssTemplate ossTemplate;

    @Override
    @Transactional
    public Resource upload(MultipartFile file, ResourcePool pool) {
        FileValidator.validate(file);
        if (pool == null) {
            pool = ResourcePool.MISC;
        }

        String originalName = file.getOriginalFilename();
        String suffix = "";
        if (originalName != null && originalName.contains(".")) {
            suffix = originalName.substring(originalName.lastIndexOf("."));
        }
        String key = pool.name().toLowerCase() + "/" + UUID.randomUUID() + suffix;

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            throw new BusinessException("读取上传文件失败");
        }

        ossTemplate.upload(key, content, file.getContentType());
        String url = ossTemplate.getUrl(key);

        Resource resource = new Resource();
        resource.setOriginalName(originalName != null ? originalName : key);
        resource.setUrl(url);
        resource.setSize((long) content.length);
        resource.setMimeType(file.getContentType());
        resource.setPool(pool);
        resource.setRefCount(0);
        resource.setStatus(ResourceStatus.ACTIVE);
        resource.setCreatedAt(LocalDateTime.now());
        resourceMapper.insert(resource);
        return resource;
    }

    @Override
    public ResourceDownload download(Long resourceId) {
        Resource resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new BusinessException("资源不存在");
        }
        boolean publicAccess = resource.getRefCount() != null && resource.getRefCount() > 0
                && resource.getStatus() == ResourceStatus.ACTIVE;
        InputStream stream = ossTemplate.download(resource.getUrl());
        return new ResourceDownload(stream, resource.getMimeType(), resource.getSize(), resource.getOriginalName(), publicAccess);
    }

    @Override
    @Transactional
    public void bind(Long resourceId, String module, Long objectId) {
        Resource resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            log.warn("[Resource] bind 跳过：资源不存在 resourceId={} module={} objectId={}", resourceId, module, objectId);
            return;
        }
        log.info("[Resource] bind: resourceId={} (name={}) module={} objectId={} 当前 refCount={}",
                resourceId, resource.getOriginalName(), module, objectId, resource.getRefCount());
        ResourceReference ref = new ResourceReference();
        ref.setResourceId(resourceId);
        ref.setModule(module);
        ref.setObjectId(objectId);
        ref.setCreatedAt(LocalDateTime.now());
        referenceMapper.insert(ref);
        resourceMapper.incrementRefCount(resourceId);
        log.info("[Resource] bind 完成: resourceId={} refCount 已 +1", resourceId);
    }

    @Override
    @Transactional
    public void unbindByObject(String module, Long objectId) {
        List<ResourceReference> refs = referenceMapper.selectByObject(module, objectId);
        referenceMapper.deleteByObject(module, objectId);
        refs.forEach(ref -> resourceMapper.decrementRefCount(ref.getResourceId()));
    }

    @Override
    @Transactional
    public void unbindByResource(Long resourceId) {
        referenceMapper.deleteByResourceId(resourceId);
        Resource resource = resourceMapper.selectById(resourceId);
        if (resource != null) {
            resource.setRefCount(0);
            resourceMapper.updateById(resource);
        }
    }

    @Override
    public List<Resource> listOrphans(int days) {
        LocalDateTime before = LocalDateTime.now().minusDays(days);
        return resourceMapper.selectOrphans(before);
    }

    @Override
    @Transactional
    public void physicalDelete(Long resourceId) {
        Resource resource = resourceMapper.selectById(resourceId);
        if (resource == null) return;

        log.info("[Resource] 物理删除: id={} name={} url={} refCount={}",
                resourceId, resource.getOriginalName(), resource.getUrl(), resource.getRefCount());

        // 尝试 OSS 删除，失败不阻断 DB 清理
        try {
            ossTemplate.deleteByUrl(resource.getUrl());
        } catch (Exception e) {
            log.error("[Resource] OSS 删除失败（继续清理 DB 记录）: id={} url={}", resourceId, resource.getUrl(), e);
        }

        // 清理引用记录（兜底，防止 resource_reference 残留导致前端请求不存在的资源）
        referenceMapper.deleteByResourceId(resourceId);

        resourceMapper.deleteById(resourceId);
        log.info("[Resource] 物理删除完成: id={}", resourceId);
    }
}
