package cc.adabyte.blog.common.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResourcePool {
    AVATAR("头像池"),
    ARTICLE("文章资源池"),
    EMOJI("表情包池"),
    MISC("零散资源池");

    private final String label;
}
