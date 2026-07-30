package cc.adabyte.blog.boot;

import cc.adabyte.blog.common.constants.ResourcePool;
import cc.adabyte.blog.common.constants.ResourceStatus;
import cc.adabyte.blog.resource.core.cache.ResourceCacheProperties;
import cc.adabyte.blog.resource.core.cache.ResourceContentCache;
import cc.adabyte.blog.resource.core.entity.Resource;
import cc.adabyte.blog.resource.core.mapper.ResourceMapper;
import cc.adabyte.blog.resource.core.mapper.ResourceReferenceMapper;
import cc.adabyte.blog.resource.oss.OssTemplate;
import cc.adabyte.blog.resource.core.service.ResourceDownload;
import cc.adabyte.blog.resource.core.service.impl.ResourceServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 资源访问判定实时性回归测试。
 *
 * <p>线上事故：资源在 refCount=0 时被下载，publicAccess=false 随内容一起进缓存；
 * 后续绑定（refCount=1）后缓存不失效，且匿名 404 命中会刷新 expireAfterAccess，
 * 导致已绑定的文章图片对访客永久 404。修复后访问判定每次请求实时计算，
 * 缓存只保存不可变的内容字节。
 */
@DisplayName("资源访问判定实时性测试")
class ResourceAccessDecisionTest {

    private ResourceServiceImpl newService(ResourceMapper resourceMapper, OssTemplate ossTemplate) {
        ResourceCacheProperties props = new ResourceCacheProperties();
        props.setEnabled(true);
        return new ResourceServiceImpl(resourceMapper, mock(ResourceReferenceMapper.class),
                ossTemplate, new ResourceContentCache(props));
    }

    private Resource articleResource(Long id, int refCount) {
        Resource r = new Resource();
        r.setId(id);
        r.setPool(ResourcePool.ARTICLE);
        r.setStatus(ResourceStatus.ACTIVE);
        r.setRefCount(refCount);
        r.setUrl("https://bucket.oss-cn.example.com/article/x.png");
        r.setMimeType("image/png");
        r.setSize(4L);
        r.setOriginalName("x.png");
        return r;
    }

    @Test
    @DisplayName("refCount=0 缓存后，绑定再下载应立即公开（不再脏读）")
    void accessDecisionIsFreshAfterBind() {
        ResourceMapper resourceMapper = mock(ResourceMapper.class);
        OssTemplate ossTemplate = mock(OssTemplate.class);
        ResourceServiceImpl service = newService(resourceMapper, ossTemplate);

        when(resourceMapper.selectById(39L))
                .thenReturn(articleResource(39L, 0))
                .thenReturn(articleResource(39L, 1));
        when(ossTemplate.download(anyString()))
                .thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3, 4}));

        ResourceDownload before = service.download(39L);
        assertFalse(before.publicAccess(), "未引用时应对访客隐藏");

        ResourceDownload after = service.download(39L);
        assertTrue(after.publicAccess(), "绑定后应立即对访客公开（修复前此处因缓存脏读为 false）");
        assertArrayEquals(new byte[]{1, 2, 3, 4}, after.content(), "内容字节仍应来自缓存");

        verify(ossTemplate, times(1)).download(anyString());
    }

    @Test
    @DisplayName("解绑后 refCount 归零，应立即对访客隐藏")
    void accessDecisionIsFreshAfterUnbind() {
        ResourceMapper resourceMapper = mock(ResourceMapper.class);
        OssTemplate ossTemplate = mock(OssTemplate.class);
        ResourceServiceImpl service = newService(resourceMapper, ossTemplate);

        when(resourceMapper.selectById(40L))
                .thenReturn(articleResource(40L, 1))
                .thenReturn(articleResource(40L, 0));
        when(ossTemplate.download(anyString()))
                .thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3, 4}));

        assertTrue(service.download(40L).publicAccess());
        assertFalse(service.download(40L).publicAccess(), "解绑后应立即恢复私有");
    }
}
