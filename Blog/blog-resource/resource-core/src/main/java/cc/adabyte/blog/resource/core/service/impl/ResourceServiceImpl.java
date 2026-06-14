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
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
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
        InputStream stream = ossTemplate.download(resource.getUrl());
        return new ResourceDownload(stream, resource.getMimeType(), resource.getSize(), resource.getOriginalName());
    }

    @Override
    @Transactional
    public void bind(Long resourceId, String module, Long objectId) {
        ResourceReference ref = new ResourceReference();
        ref.setResourceId(resourceId);
        ref.setModule(module);
        ref.setObjectId(objectId);
        ref.setCreatedAt(LocalDateTime.now());
        referenceMapper.insert(ref);
        resourceMapper.incrementRefCount(resourceId);
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

        ossTemplate.deleteByUrl(resource.getUrl());

        resourceMapper.deleteById(resourceId);
    }
}
