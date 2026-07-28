package cc.adabyte.blog.common.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResourcePool {
    /** 头像池：站点头像、留言头像、links 图标——天然对访客公开，不依赖引用计数。 */
    AVATAR("头像池", true),
    /** 文章资源池：文章 / 动态正文图片——仅在被引用后对访客公开，避免草稿图外泄。 */
    ARTICLE("文章资源池", false),
    /** 表情包池：访客留言使用——对访客公开。 */
    EMOJI("表情包池", true),
    /** 零散资源池：默认私有，仅在被引用后对访客公开。 */
    MISC("零散资源池", false);

    private final String label;
    /** 是否默认对匿名访客公开（不依赖 ref_count）。 */
    private final boolean publicByDefault;
}
