package cc.adabyte.blog.resource.core.service;

import cc.adabyte.blog.common.constants.ResourcePool;
import cc.adabyte.blog.resource.core.entity.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResourceService {
    Resource upload(MultipartFile file, ResourcePool pool);
    ResourceDownload download(Long resourceId);
    void bind(Long resourceId, String module, Long objectId);
    void unbindByObject(String module, Long objectId);
    void unbindByResource(Long resourceId);
    List<Resource> listOrphans(int days);
    void physicalDelete(Long resourceId);
}
